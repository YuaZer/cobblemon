/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.spawning.spawner

import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools
import com.cobblemon.mod.common.api.spawning.SpawnCause
import com.cobblemon.mod.common.api.spawning.detail.EntitySpawnResult
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.spawner.PokeSnackSpawnerManager.RADIUS
import com.cobblemon.mod.common.api.spawning.spawner.PokeSnackSpawnerManager.TICKS_BETWEEN_SPAWNS
import com.cobblemon.mod.common.block.PokeSnackBlock
import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity
import com.cobblemon.mod.common.net.messages.client.effect.PokeSnackBlockParticlesPacket
import net.minecraft.server.level.ServerLevel

class PokeSnackSpawner(
    name: String,
    manager: PokeSnackSpawnerManager,
    val pokeSnackBlockEntity: PokeSnackBlockEntity,
) : FixedAreaSpawner(
    name = name,
    spawns = CobblemonSpawnPools.WORLD_SPAWN_POOL,
    manager = manager,
    world = pokeSnackBlockEntity.level as ServerLevel,
    position = pokeSnackBlockEntity.blockPos,
    horizontalRadius = RADIUS,
    verticalRadius = RADIUS,
    ticksBetweenSpawns = TICKS_BETWEEN_SPAWNS.toFloat(),
) {
    override fun run(cause: SpawnCause): List<SpawnAction<*>> {
        return super.run(cause)
    }

    override fun <R> afterSpawn(action: SpawnAction<R>, result: R) {
        super.afterSpawn(action, result)

        val level = pokeSnackBlockEntity.level
        if (level == null) return

        val pokeSnackBlockPos = pokeSnackBlockEntity.blockPos

        if (result is EntitySpawnResult) {
            result.entities.forEach { entity ->
                val entityPos = entity.blockPosition()

                PokeSnackBlockParticlesPacket(pokeSnackBlockPos, entityPos).sendToPlayersAround(
                    pokeSnackBlockPos.x.toDouble(),
                    pokeSnackBlockPos.y.toDouble(),
                    pokeSnackBlockPos.z.toDouble(),
                    64.0,
                    level.dimension(),
                )
            }
        }

        val pokeSnackBlockState = pokeSnackBlockEntity.blockState
        val pokeSnackBlock = pokeSnackBlockState.block as PokeSnackBlock
        pokeSnackBlock.eat(level, pokeSnackBlockPos, pokeSnackBlockState, null)
    }
}