/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon.ai.sensors

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.google.common.collect.ImmutableSet
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.sensing.Sensor
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.GameRules
import java.util.*
import java.util.function.Predicate

class PokemonItemSensor(
    private val width: Double = 16.0, // TODO: Can we configure sensors dynamically?
    private val height: Double = 8.0,
    private val maxTravelDistance: Double = 16.0,
) : Sensor<PokemonEntity>(30) {
    override fun requires(): MutableSet<MemoryModuleType<*>?> {
        return ImmutableSet.of<MemoryModuleType<*>?>(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM)
    }

    override fun doTick(level: ServerLevel, entity: PokemonEntity) {

        if (!level.gameRules.getBoolean(GameRules.RULE_MOBGRIEFING) || !entity.pokemon.canDropHeldItem) {
            // Mob griefing is disabled or the Pokemon cannot swap out its item, so don't bother to search
            return
        }

        val heldItemValue = entity.pokemon.species.behaviour.itemInteract.getItemPriority(entity.pokemon.heldItem())
        if (heldItemValue >= entity.pokemon.species.behaviour.itemInteract.highestPriorityItem) {
            // It's already holding the highest value item it can have, no need to look for better.
            return
        }

        val list = level.getEntitiesOfClass(
            ItemEntity::class.java,
            entity.boundingBox.inflate(width, height, width),
            Predicate { it: ItemEntity? ->
                it != null
                    && entity.wantsToPickUp(it.item)
                    && entity.hasLineOfSight(it)
                    && it.closerThan(entity, maxTravelDistance)
            }
        )

        // Find the closest item to the entity
        var nearestItemEntity : ItemEntity? = null
        var shortestDistance = Float.MAX_VALUE
        list.forEach { itemEntity ->
            val distanceToEntity = entity.distanceTo(itemEntity)
            if (distanceToEntity < shortestDistance) {
                shortestDistance = distanceToEntity
                nearestItemEntity = itemEntity
            }
        }

        val brain = entity.getBrain()
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, Optional.ofNullable(nearestItemEntity))
    }

}