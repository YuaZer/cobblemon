/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon.ai.tasks

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.google.common.collect.ImmutableMap
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.behavior.Behavior
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.memory.MemoryStatus
import net.minecraft.world.item.ItemStack

class EatHeldItemTask : Behavior<LivingEntity>(
    ImmutableMap.of(
        MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
    )
) {

    companion object {
        private const val MAX_DURATION = 60
        private const val COOLDOWN = 700
    }

    private var timelastEaten: Long = 0

    override fun checkExtraStartConditions(world: ServerLevel, entity: LivingEntity): Boolean {
        entity as PokemonEntity

        if (timelastEaten + COOLDOWN > world.gameTime) {
            return false
        }
        val itemstack = entity.pokemon.heldItem()
        return !itemstack.isEmpty && canEat(itemstack)
    }

    override fun canStillUse(world: ServerLevel, entity: LivingEntity, time: Long): Boolean {
        entity as PokemonEntity

        return !entity.pokemon.heldItem.isEmpty && !entity.pokemon.isFull()
    }

    private fun canEat(item: ItemStack): Boolean {
        return item.has(DataComponents.FOOD)
    }

    override fun start(world: ServerLevel, entity: LivingEntity, time: Long) {
        timelastEaten = time
    }


    override fun tick(world: ServerLevel, entity: LivingEntity, time: Long) {
        if (!world.isClientSide && entity.isAlive && entity.isEffectiveAi && entity is PokemonEntity) {
            val itemstack: ItemStack = entity.pokemon.heldItem()
            if (canEat(itemstack)) {
                if (this.timelastEaten + MAX_DURATION <= time) {
                    val resultItemStack = itemstack.finishUsingItem(world, entity)
                    entity.pokemon.swapHeldItem(resultItemStack)
                    this.timelastEaten = time
                    entity.pokemon.feedPokemon(1, false)
                } else if (this.timelastEaten > 0 && entity.random.nextFloat() < 0.4f) {
                    entity.playSound(entity.getEatingSound(itemstack), 1.0f, 1.0f)
                    world.broadcastEntityEvent(entity, 45.toByte())
                }
            }

        }
    }

}