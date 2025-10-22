/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.spawning.spawner

import com.cobblemon.mod.common.CobblemonPoiTypes
import com.cobblemon.mod.common.api.spawning.SpawnerManager
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.village.poi.PoiManager
import net.minecraft.world.entity.ai.village.poi.PoiType

object PokeSnackSpawnerManager : SpawnerManager() {

    val pokeSnackSpawnersMap = mutableMapOf<BlockPos, PokeSnackSpawner>()
    var validPokeSnackSpawners = mutableListOf<PokeSnackSpawner>()

    const val SEARCH_RANGE: Int = 48

    override fun getValidTickingSpawners(): List<TickingSpawner> {
        return validPokeSnackSpawners
    }

    fun updateValidSpawners(server: MinecraftServer) {
        val newValidPokeSnackSpawners = server.playerList?.players?.flatMap { player ->
            val serverLevel = player.level() as ServerLevel
            serverLevel.poiManager.findAll(
                { holder: Holder<PoiType> -> holder.`is`(CobblemonPoiTypes.POKE_SNACK_KEY) },
                { true },
                player.blockPosition(),
                SEARCH_RANGE,
                PoiManager.Occupancy.ANY
            ).toList()
        }?.toSet()
        ?.mapNotNull { pokeSnackSpawnersMap[it] }
        ?.toMutableList()

        validPokeSnackSpawners = newValidPokeSnackSpawners ?: mutableListOf<PokeSnackSpawner>()
    }

    fun registerPokeSnackSpawner(pokeSnackSpawner: PokeSnackSpawner) {
        pokeSnackSpawnersMap.put(pokeSnackSpawner.pokeSnackBlockEntity.blockPos, pokeSnackSpawner)
        super.registerSpawner(pokeSnackSpawner)
    }

    fun unregisterPokeSnackSpawner(pokeSnackSpawner: PokeSnackSpawner) {
        super.unregisterSpawner(pokeSnackSpawner)
        pokeSnackSpawnersMap.remove(pokeSnackSpawner.pokeSnackBlockEntity.blockPos, pokeSnackSpawner)
        validPokeSnackSpawners.remove(pokeSnackSpawner)
    }
}
