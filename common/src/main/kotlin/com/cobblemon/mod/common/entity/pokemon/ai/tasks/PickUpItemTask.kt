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
import net.minecraft.world.entity.ai.behavior.OneShot
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder
import net.minecraft.world.entity.ai.behavior.declarative.Trigger
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.AABB

object PickUpItemTask {
    val runtime = MoLangRuntime().setup()

    fun create(condition: Expression, maxReach: Expression, itemPriority: Expression): OneShot<PokemonEntity> = BehaviorBuilder.create {
        it.group(
            it.absent(MemoryModuleType.WALK_TARGET)
        ).apply(it) { walkTarget ->
            Trigger { _, entity, _ ->

                runtime.withQueryValue("entity", entity.asMostSpecificMoLangValue())

                val condition = runtime.resolveBoolean(condition)
                if (!condition) {
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


                //TODO: Find the value of the item that the pokemon is currently holding
                //TODO: Probably do not want to drop items that have been given by a trainer
                val highestValueSeen = if (entity is PokemonEntity && entity.pokemon.heldItem().isEmpty) 0 else 9999

                for (item in list) {
                    runtime.withQueryValue("item", item.item.asMoLangValue(entity.registryAccess()))
                    val itemValue = runtime.resolveInt(itemPriority)
                    if (itemValue > highestValueSeen) {
                        itemEntity = item
                    }
                }

                if (itemEntity == null) {
                    return@Trigger false
                }

                entity.pokemon.swapHeldItem(itemEntity.item)

                return@Trigger true
            }
        }
    }
}