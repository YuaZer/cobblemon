/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.riding.behaviour

import com.cobblemon.mod.common.DoubleJump
import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.sound.RideSoundSettingsList
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.blockPositionsAsListRounded
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.FluidTags
import net.minecraft.util.SmoothDouble
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes

/**
 * Small wrapper around a RidingBehaviour to provide sane defaults in the event that the behaviour is not active.
 *
 * @author landonjw
 */
class RidingController(
    val entity: PokemonEntity,
    val behaviours: Map<RidingStyle, RidingBehaviourSettings>) {

    var lastTransitionAge: Int = 0
        private set

    var context: ActiveRidingContext? = null
        private set

    fun tick() {
        if (entity.ticksLived - lastTransitionAge < 10) return
        val newTransition = checkForNewTransition(entity.controllingPassenger)
        val behaviourSettings = entity.pokemon.riding.behaviours?.get(newTransition)
        if (newTransition != context?.style) {
            if (newTransition != null && behaviourSettings != null) {
                val newState = RidingBehaviours.get(behaviourSettings.key).createDefaultState(behaviourSettings)
                context?.let { newState.stamina.set(it.state.stamina.get(), forced = true) }
                context = ActiveRidingContext(behaviourSettings.key, behaviourSettings, newState, newTransition)
                lastTransitionAge = entity.ticksLived
            } else {
                context = null
            }
        }

        if (context == null) {
            entity.ejectPassengers()
        }
    }

    fun getBehaviour(): RidingBehaviour<RidingBehaviourSettings, RidingBehaviourState>? {
        if (context == null) return null
        return RidingBehaviours.get(context!!.behaviour)
    }

    private fun checkForNewTransition(driver: LivingEntity?): RidingStyle? {
        when (context?.style) {
            RidingStyle.AIR -> {
                if (canTransitionToLand()) return RidingStyle.LAND
                if (canTransitionToLiquid()) return RidingStyle.LIQUID
                return RidingStyle.AIR
            }

            RidingStyle.LIQUID -> {
                if (canTransitionToAir(driver)) return RidingStyle.AIR
                if (canTransitionToLand()) return RidingStyle.LAND
                return RidingStyle.LIQUID
            }

            RidingStyle.LAND -> {
                if (canTransitionToAir(driver)) return RidingStyle.AIR
                if (canTransitionToLiquid()) return RidingStyle.LIQUID
                return RidingStyle.LAND
            }

            null -> {
                if (canTransitionToAir(driver)) return RidingStyle.AIR
                if (canTransitionToLiquid()) return RidingStyle.LIQUID
                return RidingStyle.LAND
            }
        }
    }

    private fun canTransitionToLand(): Boolean {
        if (entity.isInLiquid || entity.isUnderWater) return false
        return entity.onGround()
    }

    private fun canTransitionToLiquid(): Boolean {
        return entity.isInLiquid || entity.isUnderWater
    }

    private fun canTransitionToAir(driver: LivingEntity?): Boolean {
        if (driver != null) {
            if (driver !is DoubleJump) return false
            // check if style is water and player is submerged
            val isEyeInLiquid = entity.isEyeInFluid(FluidTags.WATER) || entity.isEyeInFluid(FluidTags.LAVA)
            if (context?.style == RidingStyle.LIQUID && isEyeInLiquid) return false
            return (driver as DoubleJump).isDoubleJumping
        }
        else {
            return !entity.onGround() && !(entity.isInLiquid || entity.isUnderWater)
        }
    }
}

class ActiveRidingContext(
    val behaviour: ResourceLocation,
    val settings: RidingBehaviourSettings,
    val state: RidingBehaviourState,
    val style: RidingStyle
)