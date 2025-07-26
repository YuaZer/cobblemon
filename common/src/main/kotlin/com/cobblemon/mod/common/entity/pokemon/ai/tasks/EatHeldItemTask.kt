/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon.ai.tasks

import com.cobblemon.mod.common.CobblemonMemories
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMostSpecificMoLangValue
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.entity.pokemon.ai.tasks.MoveToItemTask.runtime
import com.cobblemon.mod.common.util.asExpressionLike
import com.cobblemon.mod.common.util.resolve
import com.cobblemon.mod.common.util.withQueryValue
import com.google.common.collect.ImmutableMap
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.behavior.Behavior
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.memory.MemoryStatus
import net.minecraft.world.item.ItemStack


class EatHeldItemTask : Behavior<PokemonEntity>(
    ImmutableMap.of(
        MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT
    )
) {

    companion object {
        private const val MAX_DURATION = 60
        private const val COOLDOWN = 700
    }

    private var timelastEaten: Long = 0

    override fun checkExtraStartConditions(world: ServerLevel, entity: PokemonEntity): Boolean {
        if (timelastEaten + COOLDOWN > world.gameTime) {
            return false
        }
        val itemstack = entity.pokemon.heldItem()
        return !itemstack.isEmpty && canEat(itemstack, entity) && !entity.isBusy
    }

    override fun canStillUse(world: ServerLevel, entity: PokemonEntity, time: Long): Boolean {
        return !entity.pokemon.heldItem.isEmpty && !entity.pokemon.isFull() && !entity.isBusy
    }

    private fun canEat(item: ItemStack, entity: PokemonEntity): Boolean {
        return item.has(DataComponents.FOOD) || entity.pokemon.species.behaviour.itemInteract.getOnUseEffect(item) != null
    }

    override fun start(world: ServerLevel, entity: PokemonEntity, time: Long) {
        timelastEaten = time
        entity.brain.setMemory(CobblemonMemories.IS_CONSUMING_ITEM, true)
    }

    override fun stop(level: ServerLevel, entity: PokemonEntity, gameTime: Long) {
        entity.brain.eraseMemory(CobblemonMemories.IS_CONSUMING_ITEM)
    }

    override fun tick(world: ServerLevel, entity: PokemonEntity, time: Long) {
        if (!world.isClientSide && entity.isAlive && entity.isEffectiveAi) {
            val itemStack: ItemStack = entity.pokemon.heldItem()
            val itemConfig = entity.pokemon.species.behaviour.itemInteract.getMatchingEntry(itemStack)
            if (canEat(itemStack, entity)) {
                if (this.timelastEaten + MAX_DURATION <= time) {
                    var resultItemStack = itemStack.finishUsingItem(world, entity)
                    val configuredReturnItem = itemConfig?.returnItem
                    resultItemStack = if (configuredReturnItem != null ) ItemStack(BuiltInRegistries.ITEM.get(configuredReturnItem))
                        else if (resultItemStack.item == itemStack.item)
                            ItemStack.EMPTY // Items that aren't normally edible return themselves
                        else
                            resultItemStack
                    entity.pokemon.swapHeldItem(resultItemStack)
                    if (itemConfig != null && itemConfig.onUseEffect != null) {
                        runtime.withQueryValue("entity", entity.asMostSpecificMoLangValue())
                        runtime.resolve(itemConfig.onUseEffect.asExpressionLike())
                    }
                    this.timelastEaten = time
                    if (itemConfig != null && itemConfig.fullnessValue > 0) {
                        entity.pokemon.feedPokemon(itemConfig.fullnessValue, false)
                    }
                } else if (itemConfig?.isFood == true && this.timelastEaten > 0 && entity.random.nextFloat() < 0.4f) {
                    entity.playSound(entity.getEatingSound(itemStack), 1.0f, 1.0f)
                    world.broadcastEntityEvent(entity, 45.toByte())
                    spawnFoodParticles(entity, itemStack)
                }
            }

        }
    }

    private fun spawnFoodParticles(entity: LivingEntity, itemStack: ItemStack) {
        val serverLevel = entity.level() as ServerLevel
        //TODO: Figure out how to this with snowstorm so we can use mouth/face/head locators
        serverLevel.sendParticles(
            ItemParticleOption(ParticleTypes.ITEM, itemStack),
            entity.x, entity.y + 0.5, entity.z, // particle position (centered on entity)
            5, // count
            0.1, 0.1, 0.1, // x, y, z offset spread
            0.05 // speed
        )
    }


}