/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon.ai.sensors

import com.cobblemon.mod.common.CobblemonMemories
import com.cobblemon.mod.common.block.SaccharineLeafBlock
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.PoiTypeTags
import net.minecraft.world.entity.ai.sensing.Sensor
import net.minecraft.world.entity.ai.village.poi.PoiManager
import net.minecraft.world.entity.ai.village.poi.PoiRecord
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BeehiveBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BeehiveBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity


// TODO: Clean up, separate hives from honey-able leaves
class BeeHiveSensor : Sensor<PokemonEntity>(300) {
    override fun requires() = setOf(CobblemonMemories.HIVE_LOCATION)
    override fun doTick(world: ServerLevel, entity: PokemonEntity) {
        val brain = entity.brain
        val currentHive = brain.getMemory(CobblemonMemories.HIVE_LOCATION).orElse(null)

        if (currentHive != null) {
            val state = world.getBlockState(currentHive)

            if (!isValidHiveOrLeaf(state) || !hasReachableAdjacentSide(world, currentHive)) {
                brain.eraseMemory(CobblemonMemories.HIVE_LOCATION)
            } else if (isAtMaxHoney(state)) {
                brain.eraseMemory(CobblemonMemories.HIVE_LOCATION)
            } else {
                return // We already have a valid hive, no need to search for another
            }
        }

        // Search for nearest hive/nest
        val searchRadius = 20  // is this too big for us to use?
        val centerPos = entity.blockPosition()

        var closestHivePos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE

        val list = findNearbyHivesWithSpace(entity)

        if (!list.isEmpty()) {
            for (blockPos in list) {
//                if (!this@Bee.goToHiveGoal.isTargetBlacklisted(blockPos)) {
//                    this@Bee.hivePos = blockPos
//                    return
//                }
                if (hasReachableAdjacentSide(world, blockPos)) {
                    closestHivePos = blockPos
                    break
                }

            }

//            this@Bee.goToHiveGoal.clearBlacklist()
//            this@Bee.hivePos = list.get(0) as BlockPos?
        }
//        BlockPos.betweenClosedStream(
//            centerPos.offset(-searchRadius, -2, -searchRadius),
//            centerPos.offset(searchRadius, 2, searchRadius)
//        ).forEach { pos ->
//            val state = world.getBlockState(pos)
//
//            if (isValidHiveOrLeaf(state) && !isAtMaxHoney(state) && hasReachableAdjacentSide(world, entity, pos)) {
//                val distance = pos.distToCenterSqr(centerPos.x + 0.5, centerPos.y + 0.5, centerPos.z + 0.5)
//                if (distance < closestDistance) {
//                    closestDistance = distance
//                    closestHivePos = pos.immutable()
//                }
//            }
//        }

        if (closestHivePos != null) {
            brain.setMemory(CobblemonMemories.HIVE_LOCATION, closestHivePos)
        }
    }

    private fun isAtMaxHoney(state: net.minecraft.world.level.block.state.BlockState): Boolean {
        return when {
            isHiveBlock(state) -> state.getValue(BeehiveBlock.HONEY_LEVEL) == BeehiveBlock.MAX_HONEY_LEVELS
            isSaccharineLeafBlock(state) -> state.getValue(SaccharineLeafBlock.AGE) == SaccharineLeafBlock.MAX_AGE
            else -> true // Unknown block type
        }
    }

    private fun isValidHiveOrLeaf(state: net.minecraft.world.level.block.state.BlockState): Boolean {
        return isHiveBlock(state) || isSaccharineLeafBlock(state)
    }

    private fun isSaccharineLeafBlock(state: net.minecraft.world.level.block.state.BlockState): Boolean {
        return state.block is SaccharineLeafBlock
    }

    private fun isHiveBlock(state: net.minecraft.world.level.block.state.BlockState): Boolean {
        return state.`is`(Blocks.BEEHIVE) || state.`is`(Blocks.BEE_NEST)
    }

    private fun hasReachableAdjacentSide(world: ServerLevel, pos: BlockPos): Boolean {
        for (dir in Direction.entries) {
            val adjacentPos = pos.relative(dir)
            if (world.isEmptyBlock(adjacentPos)) {
                return true
            }
        }
        return false
    }

    private fun findNearbyHivesWithSpace(entity: PokemonEntity): List<BlockPos> {
        val blockPos = entity.blockPosition()
        val poiManager = (entity.level() as ServerLevel).poiManager
        val stream = poiManager.getInRange(
            { holder -> holder.`is`(PoiTypeTags.BEE_HOME) },
            blockPos,
            20,
            PoiManager.Occupancy.ANY
        )
        return stream
            .map(PoiRecord::getPos)
            .filter { pos -> doesHiveHaveSpace(pos, entity.level()) }
            .sorted { pos1, pos2 -> pos1.distSqr(blockPos).toInt() - pos2.distSqr(blockPos).toInt() }
            .toList()
    }

    private fun doesHiveHaveSpace(hivePos: BlockPos, world: Level): Boolean {
        val blockEntity: BlockEntity? = world.getBlockEntity(hivePos)
        if (blockEntity is BeehiveBlockEntity) {
            return !blockEntity.isFull
        } else {
            return false
        }
    }
}