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
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMostSpecificMoLangValue
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.*
import net.minecraft.world.entity.ai.behavior.EntityTracker
import net.minecraft.world.entity.ai.behavior.OneShot
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder
import net.minecraft.world.entity.ai.behavior.declarative.Trigger
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.memory.WalkTarget
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.AABB

object MoveToItemTask {
    val runtime = MoLangRuntime().setup()

    fun create(condition: Expression, maxDistance: Expression, itemPriority: Expression, speedMultiplier: Expression): OneShot<PokemonEntity> = BehaviorBuilder.create {
        it.group(
            it.absent(MemoryModuleType.WALK_TARGET)
        ).apply(it) { walkTarget ->
            Trigger { _, entity, _ ->

                runtime.withQueryValue("entity", entity.asMostSpecificMoLangValue())

                val _condition = runtime.resolveBoolean(condition)
                if (!_condition || !entity.pokemon.canDropHeldItem) {
                    return@Trigger false // condition failed or Pokemon has been given an item to keep safe
                }

                val _maxDistance = runtime.resolveDouble(maxDistance)


                val searchBox = AABB(
                        entity.x - _maxDistance,
                        entity.y - _maxDistance,
                        entity.z - _maxDistance,
                        entity.x + _maxDistance,
                        entity.y + _maxDistance,
                        entity.z + _maxDistance)
                val list: List<ItemEntity> = entity.level().getEntitiesOfClass(ItemEntity::class.java, searchBox) { true }

                var itemEntity: ItemEntity? = null
                val highestValueSeen = entity.pokemon.species.behaviour.itemInteract.getItemPriority(entity.pokemon.heldItem.copy())

                println(entity.pokemon.heldItem.copy().displayName.string + "\t" + highestValueSeen)
                for (item in list) {
                    val itemValue = entity.pokemon.species.behaviour.itemInteract.getItemPriority(item.item)
                    if (itemValue > highestValueSeen) {
                        itemEntity = item
                    }
                }

                if (itemEntity == null) {
                    return@Trigger false
                }

                val _speedMultiplier = runtime.resolveFloat(speedMultiplier)

                entity.brain.setMemory(MemoryModuleType.LOOK_TARGET, EntityTracker(itemEntity, true))
                entity.brain.setMemory(MemoryModuleType.WALK_TARGET, WalkTarget(itemEntity, _speedMultiplier, 0))
                return@Trigger true
            }
        }
    }
}