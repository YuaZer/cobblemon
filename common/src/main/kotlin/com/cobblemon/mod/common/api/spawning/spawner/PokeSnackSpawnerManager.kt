/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.spawning.spawner

import com.cobblemon.mod.common.CobblemonPoiTypes
import com.cobblemon.mod.common.api.fishing.SpawnBait
import com.cobblemon.mod.common.api.spawning.SpawnerManager
import com.cobblemon.mod.common.api.spawning.influence.BucketMultiplyingInfluence
import com.cobblemon.mod.common.api.spawning.influence.BucketNormalizingInfluence
import com.cobblemon.mod.common.api.spawning.influence.SpawnBaitInfluence
import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.village.poi.PoiManager
import net.minecraft.world.entity.ai.village.poi.PoiType

object PokeSnackSpawnerManager : SpawnerManager() {

    private val pokeSnackSpawnersMap = mutableMapOf<BlockPos, PokeSnackSpawner>()
    private var validPokeSnackSpawners = mutableListOf<PokeSnackSpawner>()

    private val bucketMultipliers = mapOf(
        "uncommon" to 2.25f,
        "rare" to 5.5f,
        "ultra-rare" to 5.5f
    )

    const val SEARCH_RANGE: Int = 48

    override fun getValidTickingSpawners(): List<TickingSpawner> {
        return validPokeSnackSpawners.toList()
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

    fun registerPokeSnackSpawner(pokeSnackBlockEntity: PokeSnackBlockEntity): PokeSnackSpawner {
        val newPokeSnackSpawner = PokeSnackSpawner(
            name = "poke_snack_spawner_${pokeSnackBlockEntity.blockPos}",
            manager = PokeSnackSpawnerManager,
            pokeSnackBlockEntity = pokeSnackBlockEntity,
        )

        val baitEffects = pokeSnackBlockEntity.getBaitEffects()
        val highestLureTier = baitEffects.filter { it.type == SpawnBait.Effects.RARITY_BUCKET }.maxOfOrNull { it.value }?.toInt() ?: 0

        if (highestLureTier > 0) {
            val bucketNormalizingInfluence = BucketNormalizingInfluence(tier = highestLureTier)
            newPokeSnackSpawner.influences.add(bucketNormalizingInfluence)
        }

        val bucketMultiplyingInfluence = BucketMultiplyingInfluence(bucketMultipliers)
        newPokeSnackSpawner.influences.add(bucketMultiplyingInfluence)

        val seasoningsInfluence = SpawnBaitInfluence(effects = pokeSnackBlockEntity.getBaitEffects())
        newPokeSnackSpawner.influences.add(seasoningsInfluence)

        pokeSnackBlockEntity.ticksUntilNextSpawn?.let {
            newPokeSnackSpawner.ticksBetweenSpawns = it.toFloat()
        }

        pokeSnackSpawnersMap.put(pokeSnackBlockEntity.blockPos, newPokeSnackSpawner)
        super.registerSpawner(newPokeSnackSpawner)

        return newPokeSnackSpawner
    }

    fun unregisterPokeSnackSpawner(pokeSnackSpawner: PokeSnackSpawner) {
        super.unregisterSpawner(pokeSnackSpawner)
        pokeSnackSpawnersMap.remove(pokeSnackSpawner.pokeSnackBlockEntity.blockPos, pokeSnackSpawner)
        validPokeSnackSpawners.remove(pokeSnackSpawner)
    }
}
