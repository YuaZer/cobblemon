/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.camera

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.orientation.OrientationController
import com.cobblemon.mod.common.api.orientation.OrientationController.Companion.FORWARDS
import com.cobblemon.mod.common.api.orientation.OrientationController.Companion.LEFT
import com.cobblemon.mod.common.api.orientation.OrientationController.Companion.UP
import com.cobblemon.mod.common.client.MountedPokemonAnimationRenderController.setup
import com.cobblemon.mod.common.client.RidingCameraInterface
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository.getPoser
import com.cobblemon.mod.common.duck.RidePassenger
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.mixin.accessor.CameraAccessor
import com.mojang.blaze3d.Blaze3D
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.spongepowered.asm.mixin.Unique
import kotlin.math.min
import kotlin.math.sign

object MountedCameraRenderer
{
    fun getCameraPosition(
        instance: Camera,
        driver: Entity,
        vehicle: Entity,
        x: Double,
        y: Double,
        z: Double,
        thirdPersonReverse: Boolean,
        eyeHeightOld: Double,
        eyeHeight: Double
    ): Vec3? {
        if (vehicle !is PokemonEntity) {
            return null
        }
        val delegate = vehicle.delegate as? PokemonClientDelegate ?: return null

        val entityPos = Vec3(
            Mth.lerp(instance.partialTickTime.toDouble(), vehicle.xOld, vehicle.x),
            Mth.lerp(instance.partialTickTime.toDouble(), vehicle.yOld, vehicle.y),
            Mth.lerp(instance.partialTickTime.toDouble(), vehicle.zOld, vehicle.z)
        )
        setup(vehicle, instance.partialTickTime)
        val rollable = vehicle as OrientationControllable
        val vehicleController = rollable.getOrientationController()

        val model = getPoser(vehicle.pokemon.species.resourceIdentifier, FloatingState())

        val locatorName = delegate.getSeatLocator(driver)
        val locator = delegate.locatorStates[locatorName] ?: return null

        val currEyeHeight: Double = Mth.lerp(instance.partialTickTime.toDouble(), eyeHeight, eyeHeightOld)
        var offset = Vector3f(0f, (currEyeHeight - (driver.bbHeight / 2)).toFloat(), 0f)
        var eyeOffset = Vector3f(0f, (currEyeHeight - (driver.bbHeight / 2)).toFloat(), 0f)

        // Get additional offset from poser and add to the eyeHeight offset
        val shouldFlip = !(vehicleController.active && vehicleController.orientation != null) // Do not flip the offset for 3rd person reverse unless we are using normal mc camera rotation.

        eyeOffset.add(getFirstPersonOffset(model, locatorName))
        if (!instance.isDetached) {
            offset.add(
                getFirstPersonOffset(model, locatorName)
            )
        } else if( Cobblemon.config.thirdPersonViewBobbing) {
            offset.add(
                getThirdPersonOffset(thirdPersonReverse, model.thirdPersonCameraOffset, locatorName, shouldFlip)
            )
        } else {
            offset.add(
                getThirdPersonOffset(thirdPersonReverse, model.thirdPersonCameraOffsetNoViewBobbing, locatorName, shouldFlip)
            )
        }

        // Grab the current camera rotation to use as the offset rotation.
        val offsetRot = instance.rotation()
        offset = offsetRot.transform(offset)
        eyeOffset = offsetRot.transform(eyeOffset)

        val eyeLocatorOffset = Vec3(locator.matrix.getTranslation(Vector3f()))
        val eyePos = eyeLocatorOffset.add(entityPos).toVector3f()

        // Get the camera position. If 3rd person viewbobbing is enabled or the player is
        // in first person then base the camera position off the seat locator offset
        val pos = if (!instance.isDetached || Cobblemon.config.thirdPersonViewBobbing) {
                val locatorOffset = Vec3(locator.matrix.getTranslation(Vector3f()))
                locatorOffset.add(entityPos).toVector3f()
            } else {
                val pokemonCenter = Vector3f(0f, driver.bbHeight/2, 0f)
                entityPos.toVector3f().add(pokemonCenter)
            }

        val offsetDistance = offset.length()
        val offsetDirection = offset.mul(1 / offsetDistance)

        val eyeOffsetDistance = eyeOffset.length()
        val eyeOffsetDirection = eyeOffset.mul(1 / eyeOffsetDistance)

        // Use getMaxZoom to calculate clipping
        val maxZoom: Float = getMaxZoom(offsetDistance, offsetDirection, pos, instance)
        val eyeMaxZoom: Float = getMaxZoom(eyeOffsetDistance, eyeOffsetDirection, eyePos, instance)

        val playerRotater = driver as RidePassenger
        playerRotater.`cobblemon$setRideEyePos`(Vec3(eyeOffsetDirection.mul(eyeMaxZoom).add(eyePos)).add(0.0, 2.0, 0.0))
        return Vec3(offsetDirection.mul(maxZoom).add(pos))
    }

    fun setRotation(instance: Camera): Boolean {
        val accessor = instance as Any as CameraAccessor
        val ridingCameraInterface = instance as Any as RidingCameraInterface

        // Calculate the current frametime
        val d = Blaze3D.getTime()
        ridingCameraInterface.`setCobblemon$frameTime`(
            if (ridingCameraInterface.`getCobblemon$lastHandledRotationTime`() != Double.Companion.MIN_VALUE) d - ridingCameraInterface.`getCobblemon$lastHandledRotationTime`()
            else 0.0
        )
        ridingCameraInterface.`setCobblemon$lastHandledRotationTime`(d)


        // Don't assume the camera to be attached to an entity. Ponder Scenes from create et.al. for example aren't.
        if (accessor.entity == null) return false
        val vehicle: Entity? = accessor.entity.vehicle
        if (vehicle !is OrientationControllable) return false

        // If the current vehicle has no orientation then return and perform
        // vanilla camera rotations
        val vehicleController = vehicle.getOrientationController()
        if (vehicleController.orientation == null) return false


        // If the controller has been deactivated but the orientation isn't null yet
        // then perform transition from rolled ride/camera to vanilla
        if (!vehicleController.isActive() && vehicleController.orientation != null) {
            return applyTransitionRotation(vehicleController, instance)
        } else { // Otherwise perform cobblemon specific camera rotation and set transition helper variables
            applyCameraRotation(instance)
            ridingCameraInterface.`setCobblemon$returnTimer`(0f)
            ridingCameraInterface.`setCobblemon$rollAngleStart`(instance.rotation().getEulerAnglesYXZ(Vector3f()).z())
            return true
        }
    }

    fun applyCameraRotation(instance: Camera) {
        val accessor = instance as Any as CameraAccessor
        val ridingCameraInterface = instance as Any as RidingCameraInterface

        val driver = accessor.entity ?: return
        val vehicle = driver.vehicle ?: return
        val controllableVehicle = vehicle as? OrientationControllable ?: return
        if (vehicle.getOrientationController().orientation == null || !controllableVehicle.getOrientationController().active) return

        // Grab the driver and vehicle orientation controllers.
        val vehicleController: OrientationController = controllableVehicle.getOrientationController()

        val isDriving = driver.controlledVehicle != null


        // Grab the ride orientation
        // If the player is driving then use the current orientation since the driver sets it.
        // If not then use the smoothed render orientation for passengers otherwise the vehicle rotation will be jerky
        var newRotation = if (isDriving) {
             vehicleController.orientation!!.normal(Matrix3f()).getNormalizedRotation(Quaternionf())
        } else {
            vehicleController.getRenderOrientation(ridingCameraInterface.`getCobblemon$partialTickTime`())
        }


        // Unroll the current rotation if no roll is requested
        if (Cobblemon.config.disableRoll) {
            val eulerAngs = newRotation.getEulerAnglesYXZ(Vector3f())
            val currAngs: Vector3f = accessor.rotation.getEulerAnglesYXZ(Vector3f())
            val vehicle = driver.getVehicle() as PokemonEntity?
            val vel = vehicle!!.ridingAnimationData.velocitySpring.getInterpolated(ridingCameraInterface.`getCobblemon$partialTickTime`().toDouble(), 0)

            // lerp the new rotation towards this y rot at a rate proportional to the horizontal movement speed?
            val yaw = Mth.wrapDegrees(Math.toDegrees(Mth.atan2(vel.x(), vel.z())) + 180.0f)

            val degDiff = Mth.degreesDifference(Math.toDegrees(currAngs.y().toDouble()).toFloat(), yaw.toFloat())
            val pitchDeadzoneDeg = 15.0 // x degrees of deadzone about +-90 degrees of pitch.
            val pitchDeg = vehicleController.pitch
            // ConsiderPitch to be already 90 if within the deadzone around it.
            val cutoffPitch = min(1.0, (Mth.abs(pitchDeg) / (90.0f - pitchDeadzoneDeg))) * 90.0f * sign(pitchDeg)
            val lerpRateMod = Mth.cos(Math.toRadians(cutoffPitch).toFloat())

            // Apply smoothing from the cameras current yaw to the yaw of the velocity vector of the ride.
            val k = 5.0f // Some smoothing constant k
            val smoothingFactor: Double = lerpRateMod * ridingCameraInterface.`getCobblemon$frameTime`() * k
            val newYaw = currAngs.y() + (Math.toRadians(degDiff.toDouble()) * smoothingFactor)

            newRotation.set(Quaternionf().rotateYXZ(newYaw.toFloat(), eulerAngs.x(), 0.0f))
        }

        var rotationOffset = 0
        if (Minecraft.getInstance().options.getCameraType().isMirrored()) {
            newRotation.rotateY(Math.toRadians(180.0).toFloat())
            rotationOffset = 180
        }


        // Get the drivers local look angle in relation to the ride and apply it.
        if (vehicleController.active) {
            val vehicleMatrix = Matrix3f().set(newRotation)
            val driverMatrix = Matrix3f() // Base the driver
            val playerRotater = driver as RidePassenger
            newRotation = vehicleMatrix.mul(
                driverMatrix.rotateYXZ(
                    -Mth.DEG_TO_RAD * (playerRotater.`cobblemon$getRideYRot`()),
                    -Mth.DEG_TO_RAD * playerRotater.`cobblemon$getRideXRot`(),
                    0.0f
                )
            ).normal(Matrix3f()).getNormalizedRotation(Quaternionf())
        }
        accessor.rotation.set(newRotation)
        val eulerAngs = newRotation.getEulerAnglesXYZ(Vector3f())
        accessor.xRot = eulerAngs.x() * Mth.RAD_TO_DEG
        accessor.yRot = eulerAngs.y() * Mth.RAD_TO_DEG

        // Set the drivers rotations to match
        val driverEulerAngs: Vector3f = Quaternionf(accessor.rotation).getEulerAnglesYXZ(Vector3f())
        driver.xRot = -driverEulerAngs.x() * Mth.RAD_TO_DEG + rotationOffset
        driver.yRot = 180.0f - (driverEulerAngs.y() * Mth.RAD_TO_DEG)

        FORWARDS.rotate(accessor.rotation, accessor.forwards)
        UP.rotate(accessor.rotation, accessor.up)
        LEFT.rotate(accessor.rotation, accessor.left)
    }

    @Unique
    private fun applyTransitionRotation(
        vehicleController: OrientationController,
       instance: Camera
    ): Boolean {
        val accessor = instance as Any as CameraAccessor
        val ridingCameraInterface = instance as Any as RidingCameraInterface
        // If the transition has just started then
        if (ridingCameraInterface.`getCobblemon$returnTimer`().toDouble() == 0.0) {
            resetDriverRotations(instance, accessor.entity)
        }

        if (ridingCameraInterface.`getCobblemon$returnTimer`() < 1) {
            //Rotation is taken from entity since we no longer handle mouse ourselves
            //Stops a period of time when you can't input anything.
            val interpolatedRoll = Mth.lerp(ridingCameraInterface.`getCobblemon$returnTimer`(), ridingCameraInterface.`getCobblemon$rollAngleStart`(), 0.0f)
            val pitch = Math.toRadians(-accessor.entity.xRot.toDouble()).toFloat()
            val yaw = Math.toRadians((180 - accessor.entity.yRot).toDouble()).toFloat()

            val interRot = Quaternionf()
            interRot.rotationYXZ(yaw, pitch, interpolatedRoll)

            accessor.rotation.set(interRot)
            val eulerAngs = interRot.getEulerAnglesXYZ(Vector3f())
            accessor.xRot = eulerAngs.x() * Mth.RAD_TO_DEG
            accessor.yRot = eulerAngs.y() * Mth.RAD_TO_DEG
            FORWARDS.rotate(accessor.rotation, accessor.forwards)
            UP.rotate(accessor.rotation, accessor.up)
            LEFT.rotate(accessor.rotation, accessor.left)

            if (ridingCameraInterface.`getCobblemon$rollAngleStart`() == 0f) {
                ridingCameraInterface.`setCobblemon$rollAngleStart`(1f)
                vehicleController.reset()
                resetDriverRotations(instance, accessor.entity)
                return false
            }
            ridingCameraInterface.`setCobblemon$returnTimer`(ridingCameraInterface.`getCobblemon$returnTimer`() + (ridingCameraInterface.`getCobblemon$partialTickTime`() * (1.0f / 20.0f)))
            return true
        } else {
            ridingCameraInterface.`setCobblemon$returnTimer`(1f)
            vehicleController.reset()
            resetDriverRotations(instance, accessor.entity)
            return false
        }
    }

    fun resetDriverRotations(
        instance: Camera,
        driver: Entity,
    ) {
        val eulerAngs = Quaternionf(instance.rotation()).getEulerAnglesYXZ(Vector3f())
        val playerRotater = driver as RidePassenger
        // Backticks because kotlin explodes otherwise due to syntax
        playerRotater.`cobblemon$setRideXRot`(-eulerAngs.x() * Mth.RAD_TO_DEG)
        playerRotater.`cobblemon$setRideYRot`(180.0f - (eulerAngs.y() * Mth.RAD_TO_DEG))
    }


    private fun getThirdPersonOffset(
        thirdPersonReverse: Boolean,
        cameraOffsets: Map<String, Vec3>,
        locatorName: String,
        shouldFlip: Boolean = true
    ): Vector3f {
        val offset = if (thirdPersonReverse && cameraOffsets.containsKey(locatorName + "_reverse")) {
            cameraOffsets[locatorName + "_reverse"]!!.toVector3f()
        } else if (cameraOffsets.containsKey(locatorName)) {
            cameraOffsets[locatorName]!!.toVector3f()
        } else {
            Vector3f(0f, 2f, 4f)
        }

        // Don't need to account for this since orientation is derived from camera rotation and that z is already flipped.
        // Flip only when not on a rollable.
        if (thirdPersonReverse && shouldFlip) offset.z *= -1
        return offset
    }

    private fun getFirstPersonOffset(model: PosableModel, locatorName: String): Vector3f {
        val cameraOffsets = model.firstPersonCameraOffset

        return if (cameraOffsets.containsKey(locatorName)) {
            cameraOffsets[locatorName]!!.toVector3f()
        } else {
            Vector3f(0f, 0f, 0f)
        }
    }


    private fun getMaxZoom(maxZoom: Float, directionVector: Vector3f, positionVector: Vector3f, instance: Camera): Float {
        var maxZoom = maxZoom
        val pos = Vec3(positionVector)
        for (i in 0 .. 7) {
            val g = ((i and 1) * 2 - 1).toFloat()
            val h = ((i shr 1 and 1) * 2 - 1).toFloat()
            val j = ((i shr 2 and 1) * 2 - 1).toFloat()
            val vec3 = pos.add((g * 0.1f).toDouble(), (h * 0.1f).toDouble(), (j * 0.1f).toDouble())
            val vec32 = vec3.add(Vec3(directionVector).scale(maxZoom.toDouble()))
            val level = (instance as CameraAccessor).level
            val hitResult: HitResult =
                level.clip(ClipContext(vec3, vec32, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, instance.entity))
            if (hitResult.type != HitResult.Type.MISS) {
                val k = hitResult.getLocation().distanceToSqr(pos).toFloat()
                if (k < Mth.square(maxZoom)) {
                    maxZoom = Mth.sqrt(k)
                }
            }
        }

        return maxZoom
    }
}