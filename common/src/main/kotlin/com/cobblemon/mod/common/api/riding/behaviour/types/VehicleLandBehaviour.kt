/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.riding.behaviour.types

import com.bedrockk.molang.Expression
import com.bedrockk.molang.runtime.MoLangMath.lerp
import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.behaviour.*
import com.cobblemon.mod.common.api.riding.behaviour.types.air.RocketState
import com.cobblemon.mod.common.api.riding.posing.PoseOption
import com.cobblemon.mod.common.api.riding.posing.PoseProvider
import com.cobblemon.mod.common.api.riding.sound.RideSoundSettingsList
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.*
import com.cobblemon.mod.common.util.math.geometry.toRadians
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.util.SmoothDouble
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.joml.Matrix3f
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class VehicleLandBehaviour : RidingBehaviour<VehicleLandSettings, VehicleLandState> {
    companion object {
        val KEY = cobblemonResource("land/vehicle")

        val MAX_TOP_SPEED = 1.0 // 20 bl/s
        val MIN_TOP_SPEED = 0.35 // 7 bl/s
        //val MIN_SPEED = 0.25 // 5 bl/s

        //Accel will lie between 1.0 second and 5.0 seconds
        val MAX_ACCEL = (MAX_TOP_SPEED) / (20*3) //3.0 second to max speed
        val MIN_ACCEL = (MAX_TOP_SPEED) / (20*8) // 8 seconds to max speed
    }

    override val key = KEY

    override fun getRidingStyle(settings: VehicleLandSettings, state: VehicleLandState): RidingStyle {
        return RidingStyle.LAND
    }

    val poseProvider = PoseProvider<VehicleSettings, VehicleState>(PoseType.STAND)
        .with(PoseOption(PoseType.WALK) { _, state, _ ->
            return@PoseOption abs(state.rideVelocity.get().horizontalDistance()) > 0.0
        })

    override fun isActive(settings: VehicleLandSettings, state: VehicleLandState, vehicle: PokemonEntity): Boolean {
        return Shapes.create(vehicle.boundingBox).blockPositionsAsListRounded().any {
            //Need to check other fluids
            if (vehicle.isInWater || vehicle.isUnderWater) {
                return@any false
            }
            //This might not actually work, depending on what the yPos actually is. yPos of the middle of the entity? the feet?
            if (it.y.toDouble() == (vehicle.position().y)) {
                val blockState = vehicle.level().getBlockState(it.below())
                return@any !(!blockState.isAir && blockState.fluidState.isEmpty)
            }
            true
        }
    }

    override fun pose(settings: VehicleLandSettings, state: VehicleLandState, vehicle: PokemonEntity): PoseType {
        return poseProvider.select(settings, state, vehicle)
    }

    override fun speed(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {

        if (driver.jumping) {
            state.drifting.set(true)
            if(driver.zza != 0.0f) {
                state.poweredDrifting.set(true)
            } else {
                state.poweredDrifting.set(false)
            }
        } else {
            state.drifting.set(false)
            state.poweredDrifting.set(false)
        }

        return driver.deltaMovement.length().toFloat()
    }

    override fun rotation(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: LivingEntity
    ): Vec2 {

        var newMomentum = state.turnMomentum.get().toDouble()

        // Grab turningAcceleration and divide 20 twice to get
        val turningAcceleration = (vehicle.runtime.resolveDouble(settings.handlingExpr) * 1.5 / 20.0f) / 20.0f
        val turnInput =  (driver.xxa *-1.0f) * turningAcceleration

        // Maximum of 60 degrees per second for all vehicles when not drifting
        val maxTurnMomentum = 40.0f / 20.0f

        // Base boost stats off of normal turning stats
        val driftMaxTurnMomentum = maxTurnMomentum * 3.0f
        val driftTurnInput = turnInput * 0.8f

        if(state.drifting.get()) {
            if(driver.xxa != 0.0f && abs(newMomentum + turnInput) < (driftMaxTurnMomentum)) { //If max momentum will not be exceeded then modulate
                newMomentum += driftTurnInput
            } else {
                newMomentum = lerp(newMomentum, 0.0, 0.01)
            }
        } else {
            if(driver.xxa == 0.0f ) { //If no turning input then lerp to 0
                newMomentum = lerp(newMomentum, 0.0, 0.15)
            } else if(abs(newMomentum + turnInput) > maxTurnMomentum) {
                newMomentum = lerp(newMomentum, 0.0, 0.03)
            } else { //If max momentum will not be exceeded then modulate
                newMomentum += turnInput
            }
        }

        state.turnMomentum.set(newMomentum.toFloat())
        driver.yRot += newMomentum.toFloat()
        return Vec2(driver.xRot, vehicle.yRot + newMomentum.toFloat())
    }

    override fun velocity(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player,
        input: Vec3
    ): Vec3 {
        state.rideVelocity.set(calculateRideSpaceVel(settings, state, vehicle, driver))
        return state.rideVelocity.get()
    }


    private fun calculateRideSpaceVel(
        settings: VehicleSettings,
        state: VehicleState,
        vehicle: PokemonEntity,
        driver: Player
    ): Vec3 {

        // Check to see if the ride should be walking or sprinting
        //val walkSpeed = getWalkSpeed(vehicle)
        val rideTopSpeed = vehicle.runtime.resolveDouble(settings.speedExpr) * 0.6
        val topSpeed = if (state.drifting.get()) rideTopSpeed * 0.5 else rideTopSpeed

        val accel = vehicle.runtime.resolveDouble(settings.accelerationExpr) * 0.3 * 3

        //Flag for determining if player is actively inputting
        var activeInput = false

        var newVelocity = Vec3.ZERO
        if (!state.drifting.get()) {
            newVelocity = vehicle.deltaMovement

            newVelocity = newVelocity.yRot(vehicle.yRot.toRadians())

//            val yawAligned = Matrix3f().rotateY(vehicle.yRot.toRadians())
//            newVelocity = (newVelocity.toVector3f().mul(yawAligned)).toVec3d()
        } else {
            state
            newVelocity = vehicle.deltaMovement

            // Align the direction of movement with the world coordinate space so as not to modify it through turning
            val yawAligned = Matrix3f().rotateY(vehicle.yRot.toRadians())
            newVelocity = (newVelocity.toVector3f().mul(yawAligned)).toVec3d()
        }



        //speed up and slow down based on input
        if (driver.zza != 0.0f && state.stamina.get() > 0.0) {
            //make sure it can't exceed top speed
            val forwardInput = when {
                driver.zza > 0 && newVelocity.z > topSpeed -> 0.0
                driver.zza < 0 && newVelocity.z < (-topSpeed / 3.0) -> 0.0
                else -> driver.zza.sign
            }

            newVelocity = Vec3(
                newVelocity.x,
                newVelocity.y,
                (newVelocity.z + (accel * forwardInput.toDouble())))

            activeInput = true
        }

        // Gravity logic
        if (vehicle.onGround()) {
            newVelocity = Vec3(newVelocity.x, 0.0, newVelocity.z)
        } else {
            //TODO: Should we just go back to standard minecraft gravity or do the lerp modifications prevent that?
            //I think minecrafts gravity logic is also too harsh and isn't gamefied enough for mounts maybe? Need
            //to do some testing and get other's opinions
            val gravity = (9.8 / ( 20.0)) * 0.2 * 0.15
            val terminalVel = 2.0

            val fallingForce = gravity -  ( newVelocity.z.sign *gravity *(abs(newVelocity.z) / 2.0))
            newVelocity = Vec3(newVelocity.x, max(newVelocity.y - fallingForce, -terminalVel), newVelocity.z)
        }

        //ground Friction
        var friction = 0.003
        if (state.drifting.get()) {friction *= 0.1}
        newVelocity = newVelocity.subtract(
            min(friction , abs(newVelocity.x)) * newVelocity.x.sign,
            0.0,
            min(friction , abs(newVelocity.z)) * newVelocity.z.sign
        )

        // TEST FOR LATERAL SLIP OR WHATEVER
        //TODO: calc lateral slip and friction better!
        if (!state.drifting.get()) {
            newVelocity = newVelocity.subtract(
                min(friction , abs(newVelocity.x)) * newVelocity.x.sign,
                0.0,
                0.0,
            )
        }
//        if( (newVelocity.horizontalDistance() > 0 && vehicle.onGround() && !activeInput) || newVelocity.horizontalDistance() > topSpeed) {
//
//        }


        //

//        //TODO: Change this so its tied to a jumping stat and representative of the amount of jumps
//        val canJump = vehicle.runtime.resolveBoolean(settings.canJump)
//        //Jump the thang!
//        if (driver.jumping && vehicle.onGround() && canJump) {
//            val jumpForce = 0.5
//
//            newVelocity = newVelocity.add(0.0, jumpForce, 0.0)
//
//        }
//
//        //Zero out lateral velocity possibly picked up from a controller transition
//        newVelocity = Vec3(0.0, newVelocity.y, newVelocity.z)

//        val yawAligned = Matrix3f().rotateY(vehicle.yRot.toRadians())
//        newVelocity = (newVelocity.toVector3f().mul(yawAligned)).toVec3d()

//        if (!state.drifting.get()) {
//            newVelocity = newVelocity.yRot(-vehicle.yRot.toRadians())
//        }

        return newVelocity
    }

    override fun angRollVel(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player,
        deltaTime: Double
    ): Vec3 {
        return Vec3.ZERO
    }

    override fun rotationOnMouseXY(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player,
        mouseY: Double,
        mouseX: Double,
        mouseYSmoother: SmoothDouble,
        mouseXSmoother: SmoothDouble,
        sensitivity: Double,
        deltaTime: Double
    ): Vec3 {
        if (driver !is OrientationControllable) return Vec3.ZERO

        //Might need to add the smoothing here for default.
        val invertRoll = if (Cobblemon.config.invertRoll) -1 else 1
        val invertPitch = if (Cobblemon.config.invertPitch) -1 else 1
        return Vec3(0.0, mouseY * invertPitch, mouseX * invertRoll)
    }

    override fun canJump(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player
    ): Boolean {
        return vehicle.runtime.resolveBoolean(settings.canJump)
    }

    override fun setRideBar(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        return 0.0f
    }

    override fun jumpForce(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player,
        jumpStrength: Int
    ): Vec3 {
        return Vec3.ZERO
    }

    override fun gravity(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        regularGravity: Double
    ): Double {
        return regularGravity
    }

    override fun rideFovMultiplier(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        return 1.0f
    }

    override fun useAngVelSmoothing(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun useRidingAltPose(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity,
        driver: Player
    ): ResourceLocation {
        when {
            state.poweredDrifting.get() -> return cobblemonResource("powered_drifting")
            state.drifting.get() -> return cobblemonResource("drifting")
        }
        return cobblemonResource("no_pose")
    }

    override fun inertia(settings: VehicleSettings, state: VehicleState, vehicle: PokemonEntity): Double {
        return 1.0
    }

    override fun shouldRoll(settings: VehicleLandSettings, state: VehicleLandState, vehicle: PokemonEntity): Boolean {
        return false
    }

    override fun turnOffOnGround(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun dismountOnShift(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun shouldRotatePokemonHead(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun shouldRotatePlayerHead(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun getRideSounds(
        settings: VehicleLandSettings,
        state: VehicleLandState,
        vehicle: PokemonEntity
    ): RideSoundSettingsList {
        return settings.rideSounds
    }

    override fun createDefaultState(settings: VehicleLandSettings) = VehicleLandState()
}

class VehicleLandSettings : RidingBehaviourSettings {
    override val key = VehicleLandBehaviour.KEY

    var canJump = "true".asExpression()
        private set

    var jumpVector = listOf("0".asExpression(), "0.3".asExpression(), "0".asExpression())
        private set

    var speed = "0.3".asExpression()
        private set

    var driveFactor = "1.0".asExpression()
        private set

    var reverseDriveFactor = "0.25".asExpression()
        private set

    var minimumSpeedToTurn = "0.1".asExpression()
        private set

    var rotationSpeed = "45/20".asExpression()
        private set

    var lookYawLimit = "101".asExpression()
        private set

    var speedExpr: Expression = "q.get_ride_stats('SPEED', 'LAND', 1.0, 0.3)".asExpression()
        private set

    // Max accel is a whole 1.0 in 1 second. The conversion in the function below is to convert seconds to ticks
    var accelerationExpr: Expression =
        "q.get_ride_stats('ACCELERATION', 'LAND', (1.0 / (20.0 * 1.5)), (1.0 / (20.0 * 5.0)))".asExpression()
        private set

    // Between 30 seconds and 10 seconds at the lowest when at full speed.
    var staminaExpr: Expression = "q.get_ride_stats('STAMINA', 'LAND', 30.0, 10.0)".asExpression()
        private set

    //Between a one block jump and a ten block jump
    var jumpExpr: Expression = "q.get_ride_stats('JUMP', 'LAND', 10.0, 1.0)".asExpression()
        private set

    var handlingExpr: Expression = "q.get_ride_stats('SKILL', 'LAND', 140.0, 20.0)".asExpression()
        private set

    var rideSounds: RideSoundSettingsList = RideSoundSettingsList()

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeResourceLocation(key)
        rideSounds.encode(buffer)
        buffer.writeExpression(canJump)
        buffer.writeExpression(jumpVector[0])
        buffer.writeExpression(jumpVector[1])
        buffer.writeExpression(jumpVector[2])
        buffer.writeExpression(speed)
        buffer.writeExpression(driveFactor)
        buffer.writeExpression(reverseDriveFactor)
        buffer.writeExpression(minimumSpeedToTurn)
        buffer.writeExpression(rotationSpeed)
        buffer.writeExpression(lookYawLimit)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf) {
        rideSounds = RideSoundSettingsList.decode(buffer)
        canJump = buffer.readExpression()
        jumpVector = listOf(
            buffer.readExpression(),
            buffer.readExpression(),
            buffer.readExpression()
        )
        speed = buffer.readExpression()
        driveFactor = buffer.readExpression()
        reverseDriveFactor = buffer.readExpression()
        minimumSpeedToTurn = buffer.readExpression()
        rotationSpeed = buffer.readExpression()
        lookYawLimit = buffer.readExpression()
    }
}

class VehicleLandState : RidingBehaviourState() {
    var currSpeed = ridingState(0.0, Side.BOTH)
    var deltaRotation = ridingState(Vec2.ZERO, Side.BOTH)
    var drifting = ridingState(false, Side.CLIENT)
    var poweredDrifting = ridingState(false, Side.CLIENT)
    val turnMomentum: SidedRidingState<Float> = ridingState(0.0f, Side.CLIENT)


    override fun reset() {
        super.reset()
        currSpeed.set(0.0, forced = true)
        deltaRotation.set(Vec2.ZERO, forced = true)
        drifting.set(false, forced = true)
        poweredDrifting.set(false, forced = true)
        turnMomentum.set(0.0f, forced = true)
    }

    override fun encode(buffer: FriendlyByteBuf) {
        super.encode(buffer)
        buffer.writeBoolean(drifting.get())
        buffer.writeBoolean(poweredDrifting.get())
        buffer.writeFloat(turnMomentum.get())
    }

    override fun decode(buffer: FriendlyByteBuf) {
        super.decode(buffer)
        drifting.set(buffer.readBoolean(), forced = true)
        poweredDrifting.set(buffer.readBoolean(), forced = true)
        turnMomentum.set(buffer.readFloat(), forced = true)
    }

    override fun toString(): String {
        return "VehicleLandState(currSpeed=${currSpeed.get()}, deltaRotation=${deltaRotation.get()})"
    }

    override fun copy() = VehicleLandState().also {
        it.rideVelocity.set(rideVelocity.get(), forced = true)
        it.stamina.set(stamina.get(), forced = true)
        it.currSpeed.set(currSpeed.get(), forced = true)
        it.deltaRotation.set(deltaRotation.get(), forced = true)
        it.drifting.set(drifting.get(), forced = true)
        it.poweredDrifting.set(poweredDrifting.get(), forced = true)
        it.turnMomentum.set(turnMomentum.get(), forced = true)
    }

    override fun shouldSync(previous: RidingBehaviourState): Boolean {
        if (previous !is VehicleState) return false
        if (previous.drifting.get() != drifting.get()) return true
        if (previous.poweredDrifting.get() != poweredDrifting.get()) return true
        if (previous.turnMomentum.get() != turnMomentum.get()) return true
        return super.shouldSync(previous)
    }
}
