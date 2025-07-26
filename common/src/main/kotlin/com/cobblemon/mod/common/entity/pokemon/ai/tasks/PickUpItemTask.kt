/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon.ai.tasks

import com.bedrockk.molang.Expression
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.CobblemonMemories
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMostSpecificMoLangValue
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.*
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.ai.behavior.OneShot
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder
import net.minecraft.world.entity.ai.behavior.declarative.Trigger
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB

object PickUpItemTask {
    val runtime = MoLangRuntime().setup()

    fun create(condition: Expression, maxReach: Expression, itemPriority: Expression): OneShot<PokemonEntity> = BehaviorBuilder.create {
        it.group(
            it.absent(MemoryModuleType.WALK_TARGET),
            it.absent(CobblemonMemories.IS_CONSUMING_ITEM)
        ).apply(it) { walkTarget, isConsumingItem ->
            Trigger { _, entity, _ ->

                runtime.withQueryValue("entity", entity.asMostSpecificMoLangValue())

                val condition = runtime.resolveBoolean(condition)
                if (!condition || (entity is PokemonEntity && !entity.pokemon.canDropHeldItem)) {
                    return@Trigger false
                }

                val maxReach = runtime.resolveDouble(maxReach)


                val searchBox = AABB(
                        entity.x - maxReach,
                        entity.y - maxReach,
                        entity.z - maxReach,
                        entity.x + maxReach,
                        entity.y + maxReach,
                        entity.z + maxReach)
                val list: List<ItemEntity> = entity.level().getEntitiesOfClass(ItemEntity::class.java, searchBox) { true }

                var itemEntity: ItemEntity? = null

                if (entity !is PokemonEntity) return@Trigger false

                // Probably do not want to drop items that have been given by a trainer
//                runtime.withQueryValue("item", entity.pokemon.heldItem.copy().asMoLangValue(entity.registryAccess()))
                val highestValueSeen = if (!entity.pokemon.canDropHeldItem) 9999
                    else entity.pokemon.species.behaviour.itemInteract.getItemPriority(entity.pokemon.heldItem.copy())

                for (item in list) {
//                    runtime.withQueryValue("item", item.item.asMoLangValue(entity.registryAccess()))
//                    val itemValue = runtime.resolveInt(itemPriority)
                    val itemValue = entity.pokemon.species.behaviour.itemInteract.getItemPriority(item.item)
                    if (itemValue > highestValueSeen) {
                        itemEntity = item
                    }
                }

                if (itemEntity == null) {
                    if (highestValueSeen < 0) {
                        val stack = entity.pokemon.swapHeldItem(ItemStack.EMPTY)
                        if (!entity.level().isClientSide) {
                            val itemEntity = ItemEntity(
                                entity.level(),
                                entity.x + entity.lookAngle.x,
                                entity.y + 1.0,
                                entity.z + entity.lookAngle.z,
                                stack
                            )
                            itemEntity.setPickUpDelay(40)
                            itemEntity.setThrower(entity)
                            entity.playSound(SoundEvents.FOX_SPIT, 1.0f, 1.0f)
                            entity.level().addFreshEntity(itemEntity)
                        }
                    }
                    return@Trigger false
                }
                entity.take(itemEntity, 1)
                val stack = entity.pokemon.swapHeldItem(itemEntity.item)
                if (!stack.isEmpty() && !entity.level().isClientSide) {
                    val itemEntity = ItemEntity(
                        entity.level(),
                        entity.x + entity.lookAngle.x,
                        entity.y + 1.0,
                        entity.z + entity.lookAngle.z,
                        stack
                    )
                    itemEntity.setPickUpDelay(40)
                    itemEntity.setThrower(entity)
                    entity.playSound(SoundEvents.FOX_SPIT, 1.0f, 1.0f)
                    entity.level().addFreshEntity(itemEntity)
                }

                return@Trigger true
            }
        }
    }
}