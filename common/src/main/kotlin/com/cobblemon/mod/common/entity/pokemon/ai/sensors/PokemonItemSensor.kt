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
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.sensing.Sensor
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import java.util.*
import java.util.function.Predicate
import java.util.function.ToDoubleFunction

class PokemonItemSensor(
    private val width: Double = 16.0, // TODO: Can we configure sensors dynamically?
    private val height: Double = 8.0,
    private val maxTravelDistance: Double = 16.0,
) : Sensor<PokemonEntity>(30) {
    override fun requires(): MutableSet<MemoryModuleType<*>?> {
        return ImmutableSet.of<MemoryModuleType<*>?>(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM)
    }

    override fun doTick(level: ServerLevel, entity: PokemonEntity) {

        if (!entity.pokemon.canDropHeldItem) {
            // Pokemon cannot swap out its item, so don't bother to search
            return
        }

        val heldItemValue = entity.pokemon.species.behaviour.itemInteract.getItemPriority(entity.pokemon.heldItem())
        if (heldItemValue >= entity.pokemon.species.behaviour.itemInteract.highestPriorityItem) {
            // It's already holding the highest value item it can have, no need to look for better.
            return
        }

        val brain = entity.getBrain()
        val list = level.getEntitiesOfClass(
            ItemEntity::class.java,
            entity.boundingBox.inflate(width, height, width),
            Predicate { arg: ItemEntity? -> entity.pokemon.species.behaviour.itemInteract.getItemPriority(arg?.item ?: ItemStack.EMPTY) > heldItemValue })
        Objects.requireNonNull<Mob?>(entity)
        list.sortWith(Comparator.comparingDouble(ToDoubleFunction { entity: ItemEntity? ->
            entity!!.distanceToSqr(
                entity
            )
        }))
        val filteredList = list.filter { arg2: ItemEntity? -> entity.wantsToPickUp(arg2!!.item) }
            .filter { arg2: ItemEntity? -> arg2!!.closerThan(entity, maxTravelDistance) }
        Objects.requireNonNull<Mob?>(entity)
        val nearestItem = filteredList.firstOrNull { it -> entity.hasLineOfSight(it) }
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, Optional.ofNullable(nearestItem))
    }

}