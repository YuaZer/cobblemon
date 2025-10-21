/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.spawning.spawner

import com.cobblemon.mod.common.api.spawning.SpawnerManager
import com.cobblemon.mod.common.api.spawning.influence.SpawnBaitInfluence
import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity
import com.cobblemon.mod.common.util.isServerSide
import net.minecraft.core.BlockPos

object PokeSnackSpawnerManager : SpawnerManager() {

    const val RADIUS = 3
    const val TICKS_BETWEEN_SPAWNS = 10 * 20 // 10 seconds

    val pokeSnackSpawners = mutableMapOf<BlockPos, FixedAreaSpawner>()

    fun unregisterPokeSnackSpawner(pokeSnackBlockEntity: PokeSnackBlockEntity) {
        pokeSnackSpawners.remove(pokeSnackBlockEntity.blockPos)?.let {
            unregisterSpawner(it)
        }
    }

    fun registerPokeSnackSpawner(pokeSnackBlockEntity: PokeSnackBlockEntity) {
        if (pokeSnackBlockEntity.level?.isServerSide() != true) return

        val newPokeSnackSpawner = PokeSnackSpawner(
            name = "poke_snack_spawner_${pokeSnackSpawners.size + 1}",
            manager = this,
            pokeSnackBlockEntity = pokeSnackBlockEntity,
        )

        val seasoningsInfluence = SpawnBaitInfluence(effects = pokeSnackBlockEntity.getBaitEffects())
        newPokeSnackSpawner.influences.add(seasoningsInfluence)

        pokeSnackSpawners[pokeSnackBlockEntity.blockPos] = newPokeSnackSpawner
        registerSpawner(newPokeSnackSpawner)
    }
}