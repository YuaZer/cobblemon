/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.riding.behaviour.types.air

import com.bedrockk.molang.Expression
import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.behaviour.*
import com.cobblemon.mod.common.api.riding.behaviour.types.land.HorseSettings
import com.cobblemon.mod.common.api.riding.behaviour.types.land.HorseState
import com.cobblemon.mod.common.api.riding.posing.PoseOption
import com.cobblemon.mod.common.api.riding.posing.PoseProvider
import com.cobblemon.mod.common.api.riding.sound.RideSoundSettingsList
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.*
import com.cobblemon.mod.common.util.math.geometry.toRadians
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.util.SmoothDouble
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.joml.Matrix3f
import kotlin.math.*

class HoverBehaviour : RidingBehaviour<HoverSettings, HoverState> {
    companion object {
        val KEY = cobblemonResource("air/hover")
    }

    override val key = KEY

    override fun getRidingStyle(settings: HoverSettings, state: HoverState): RidingStyle {
        return RidingStyle.AIR
    }

    val poseProvider = PoseProvider<HoverSettings, HoverState>(PoseType.STAND)
        .with(PoseOption(PoseType.WALK) { _, state, _ ->
            return@PoseOption abs(state.rideVelocity.get().z) > 0.2
        })

    override fun isActive(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ): Boolean {
        return Shapes.create(vehicle.boundingBox).blockPositionsAsListRounded().any {
            //Need to check other fluids
            if (vehicle.isInWater || vehicle.isUnderWater) {
                return@any false
            }
            //This might not actually work, depending on what the yPos actually is. yPos of the middle of the entity? the feet?
            if (it.y.toDouble() == (vehicle.position().y)) {
                val blockState = vehicle.level().getBlockState(it.below())
                return@any !blockState.isAir && blockState.fluidState.isEmpty
            }
            true
        }
    }

    override fun pose(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ): PoseType {
        return this.poseProvider.select(settings, state, vehicle)
    }

    override fun speed(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        checkTooHigh(settings, state, vehicle)
        tickStamina(settings, state, vehicle)
        return state.speed.get().toFloat()
    }

    fun checkTooHigh(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
    ) {
        val heightLimit = vehicle.runtime.resolveDouble(settings.jumpExpr)
        val pos = vehicle.position()
        val level = Minecraft.getInstance().player?.level() ?: return
        val hit = level.clip(
            ClipContext(
                pos,
                pos.subtract(0.0, heightLimit, 0.0),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                vehicle
            )
        )

        state.tooHigh.set(hit.type == HitResult.Type.MISS)
    }

    fun tickStamina(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ) {
        val stam = state.stamina.get()
        val stamDrainRate = (1.0f / vehicle.runtime.resolveDouble(settings.staminaExpr)).toFloat() / 20.0f

        val newStam = if (state.tooHigh.get()) max(0.0f,stam - stamDrainRate)
            else min(1.0f,stam + stamDrainRate * 2)

        state.stamina.set(newStam)
    }

    override fun updatePassengerRotation(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: LivingEntity
    ) {
        return
    }

    override fun clampPassengerRotation(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: LivingEntity
    ) {
        val f = Mth.wrapDegrees(driver.yRot - vehicle.yRot)
        val lookYawLimit = 90.0f
        val g = Mth.clamp(f, -lookYawLimit, lookYawLimit)
        driver.yRotO += g - f
        driver.yRot = driver.yRot + g - f
    }


    override fun rotation(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: LivingEntity
    ): Vec2 {
        val turnAmount =  calcRotAmount(settings, state, vehicle, driver)

        return Vec2(driver.xRot, vehicle.yRot + turnAmount )

    }

    /*
   *  Calculates the rotation amount given the difference between
   *  the current player y rot and entity y rot. This gives the
   *  affect of influencing a rides rotation through the mouse
   *  without it being instant.
   */
    fun calcRotAmount(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: LivingEntity
    ): Float {
        //In degrees per second
        val handling = 220.0
        val maxYawDiff = 90.0f

        //Normalize the current rotation diff
        val rotDiff = Mth.wrapDegrees(driver.yRot - vehicle.yRot)
        val rotDiffNorm = rotDiff / maxYawDiff

        //Take the square root so that the ride levels out quicker when at lower differences between entity
        //y and driver y
        //This influences the speed of the turn based on how far in one direction you're looking
        val rotDiffMod = (sqrt(abs(rotDiffNorm)) * rotDiffNorm.sign)

        //Take the inverse so that you turn less at higher speeds
        val normSpeed = 1.0f // = 1.0f - 0.5f*RidingBehaviour.scaleToRange(state.rideVelocityocity.length(), 0.0, topSpeed).toFloat()

        val turnRate = (handling.toFloat() / 20.0f)

        //Ensure you only ever rotate as much difference as there is between the angles.
        val turnSpeed = turnRate  * rotDiffMod * normSpeed
        val rotAmount = turnSpeed.coerceIn(-abs(rotDiff), abs(rotDiff))

        return rotAmount
    }

    override fun velocity(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: Player,
        input: Vec3
    ): Vec3 {
        val retVel = calculateRideSpaceVel(settings, state, vehicle, driver)
        return retVel
    }

    private fun calculateRideSpaceVel(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: Player
    ): Vec3 {
        // Convert these to their tick values
        val topSpeed = vehicle.runtime.resolveDouble(settings.speedExpr) / 20.0
        val accel = vehicle.runtime.resolveDouble(settings.accelerationExpr) / 400.0

        var newVelocity = vehicle.deltaMovement

        // Check for collisions and if found, update the speed to reflect it
        if (vehicle.verticalCollision || vehicle.horizontalCollision) {
            state.speed.set(vehicle.deltaMovement.length())
        }

        // align the velocity vector to be in local vehicle space
        val yawAligned = Matrix3f().rotateY(vehicle.yRot.toRadians())
        newVelocity = (newVelocity.toVector3f().mul(yawAligned)).toVec3d()
        if (newVelocity.length() < 0.001) {
            // Zero out the vector to ignore lingering small values that influence
            // direction of movement when scaled by stored speed value.
            newVelocity = Vec3.ZERO
        }
        newVelocity = newVelocity.normalize().scale(state.speed.get())

        // Vertical movement based on driver input.
        val vertInput = when {
            driver.jumping && state.stamina.get() != 0.0f -> 1.0
            driver.isShiftKeyDown -> -1.0
            else -> 0.0
        }

        // Normalize and scale to accel so that diagonal movement
        // doesn't speed up the ride more than input in one direction
        val inputVel = Vec3(
            driver.xxa.sign.toDouble(),
            vertInput,
            driver.zza.sign.toDouble()
        ).normalize().scale(accel)

        // Scale down input force that opposes our current movement. How much it is scaled
        // down depends on our handling/skill
//        val handlingMod = if(newVelocity.length() > 0.001) 1 - handling*sqrt(newVelocity.length() / topSpeed)*( 0.5 * (1 + newVelocity.normalize().dot(inputVel.normalize()) * -1.0))
//            else 1.0

        newVelocity = newVelocity.add(inputVel)

        // Air Friction/Resistance
        // When the dot between friction and input is -1 we want to apply none of it.
        // In short we want to avoid applying friction that opposes our input force.
        // otherwise we would not be able to reach our top speed when acceleration is
        // low.
        val frictionConst = vehicle.runtime.resolveDouble(settings.handlingExpr)
        var frictionForce = newVelocity.scale(-1.0).scale(frictionConst)
        val frictionApplied = if(inputVel.lengthSqr() != 0.0) 1 - 0.5 * (1 + -1.0 * frictionForce.normalize().dot(inputVel.normalize()))
            else 1.0
        frictionForce = frictionForce.scale((frictionApplied).pow(4))
        newVelocity = newVelocity.add(frictionForce)

        // Apply height limit forces
        if (state.tooHigh.get()) {
            newVelocity = newVelocity.scale(0.96)
            if (state.stamina.get() == 0.0f) {
                newVelocity = newVelocity.subtract(0.0, 0.01, 0.0)
            }
        }

        // Set speed to new magnitude to avoid the horrible things minecraft friction
        // does to our vectors :(
        state.speed.set(min(topSpeed, newVelocity.length()))

        return newVelocity
    }

    override fun angRollVel(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: Player,
        deltaTime: Double
    ): Vec3 {
        return Vec3.ZERO
    }

    override fun rotationOnMouseXY(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: Player,
        mouseY: Double,
        mouseX: Double,
        mouseYSmoother: SmoothDouble,
        mouseXSmoother: SmoothDouble,
        sensitivity: Double,
        deltaTime: Double
    ): Vec3 {
        return Vec3.ZERO
    }

    override fun canJump(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: Player
    ): Boolean {
        return false
    }

    override fun setRideBar(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        //Retrieve stamina from state and use it to set the "stamina bar"
        return (state.stamina.get() / 1.0f)
    }

    override fun jumpForce(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: Player,
        jumpStrength: Int
    ): Vec3 {
        return Vec3.ZERO
    }

    override fun gravity(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        regularGravity: Double
    ): Double {
        return 0.0
    }

    override fun rideFovMultiplier(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: Player
    ): Float {
        return if (state.tooHigh.get()) 0.9f else 1.0f
    }

    override fun useAngVelSmoothing(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun useRidingAltPose(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity,
        driver: Player
    ): ResourceLocation {
        return cobblemonResource("no_pose")
    }

    override fun inertia(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ): Double {
        return 1.0
    }

    override fun shouldRoll(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun turnOffOnGround(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun dismountOnShift(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun shouldRotatePokemonHead(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun shouldRotateRiderHead(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ): Boolean {
        return false
    }

    override fun getRideSounds(
        settings: HoverSettings,
        state: HoverState,
        vehicle: PokemonEntity
    ): RideSoundSettingsList {
        return settings.rideSounds
    }

    override fun createDefaultState(settings: HoverSettings) = HoverState()
}

class HoverSettings : RidingBehaviourSettings {
    override val key = HoverBehaviour.KEY
    override val stats = mutableMapOf<RidingStat, IntRange>()

    var canJump = "true".asExpression()
        private set

    // Speed in block per second
    var speedExpr: Expression = "q.get_ride_stats('SPEED', 'AIR', 13.0, 6.0)".asExpression()
        private set

    // Acceleration in blocks per second per second
    var accelerationExpr: Expression =
        "q.get_ride_stats('ACCELERATION', 'AIR', 7.0, 3.0)".asExpression()
        private set

    // Amount of seconds before stamina depletion
    var staminaExpr: Expression = "q.get_ride_stats('STAMINA', 'AIR', 7, 3)".asExpression()
        private set

    // Height in blocks above the ground before stamina loss
    var jumpExpr: Expression = "q.get_ride_stats('JUMP', 'AIR', 10.0, 5.0)".asExpression()
        private set

    // Amount of air friction applied. 0.2 means 20 percent of the velocity is opposed per tick
    var handlingExpr: Expression = "q.get_ride_stats('SKILL', 'AIR', 0.1, 0.02)".asExpression()
        private set

    var rideSounds: RideSoundSettingsList = RideSoundSettingsList()

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeResourceLocation(key)
        buffer.writeRidingStats(stats)
        rideSounds.encode(buffer)
        buffer.writeExpression(speedExpr)
        buffer.writeExpression(accelerationExpr)
        buffer.writeExpression(staminaExpr)
        buffer.writeExpression(jumpExpr)
        buffer.writeExpression(handlingExpr)
    }

    override fun decode(buffer: RegistryFriendlyByteBuf) {
        stats.putAll(buffer.readRidingStats())
        rideSounds = RideSoundSettingsList.decode(buffer)
        speedExpr = buffer.readExpression()
        accelerationExpr = buffer.readExpression()
        staminaExpr = buffer.readExpression()
        jumpExpr = buffer.readExpression()
        handlingExpr = buffer.readExpression()
    }

}

class HoverState : RidingBehaviourState() {
    var speed = ridingState(0.0, Side.CLIENT)
    var tooHigh = ridingState(false, Side.CLIENT)

    override fun encode(buffer: FriendlyByteBuf) {
        super.encode(buffer)
        buffer.writeDouble(speed.get())
        buffer.writeBoolean(tooHigh.get())
    }

    override fun decode(buffer: FriendlyByteBuf) {
        super.decode(buffer)
        speed.set(buffer.readDouble(), forced = true)
        tooHigh.set(buffer.readBoolean(), forced = true)
    }

    override fun copy() = HoverState().also {
        it.rideVelocity.set(this.rideVelocity.get(), forced = true)
        it.stamina.set(this.stamina.get(), forced = true)
        it.tooHigh.set(this.tooHigh.get(), forced = true)
    }

    override fun shouldSync(previous: RidingBehaviourState): Boolean {
        if (previous !is HoverState) return false
        return super.shouldSync(previous)
    }
}
