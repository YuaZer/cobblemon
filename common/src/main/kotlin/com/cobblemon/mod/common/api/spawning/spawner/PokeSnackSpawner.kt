/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.spawning.spawner

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.fishing.SpawnBait
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools
import com.cobblemon.mod.common.api.spawning.detail.EntitySpawnResult
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction
import com.cobblemon.mod.common.api.spawning.influence.BucketMultiplyingInfluence
import com.cobblemon.mod.common.api.spawning.influence.BucketNormalizingInfluence
import com.cobblemon.mod.common.api.spawning.influence.SpawnBaitInfluence
import com.cobblemon.mod.common.block.PokeSnackBlock
import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
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
) {

    companion object {
        const val RADIUS = 8
        const val RANDOM_TICKS_BETWEEN_SPAWNS = 4
        const val POKE_SNACK_CRUMBED_ASPECT = "poke_snack_crumbed"
    }

    init {
        val baitEffects = pokeSnackBlockEntity.getBaitEffects()

        val highestLureTier = baitEffects
            .filter { it.type == SpawnBait.Effects.RARITY_BUCKET }
            .maxOfOrNull { it.value }
            ?.toInt()
            ?: 0

        if (highestLureTier > 0) {
            influences += BucketNormalizingInfluence(tier = highestLureTier)
        }

        influences += BucketMultiplyingInfluence(
            mapOf(
                "uncommon" to 2.25f,
                "rare" to 5.5f,
                "ultra-rare" to 5.5f,
            )
        )

        influences += SpawnBaitInfluence(effects = baitEffects)

        spawnsPerPass = 1
        this.ticksUntilNextSpawn = pokeSnackBlockEntity.ticksUntilNextSpawn ?: getRandomTicksUntilNextSpawn()
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

                if (entity is PokemonEntity) {
                    entity.pokemon.forcedAspects += POKE_SNACK_CRUMBED_ASPECT
                }
            }
        }

        ticksUntilNextSpawn = getRandomTicksUntilNextSpawn()

        val pokeSnackBlockState = pokeSnackBlockEntity.blockState
        val pokeSnackBlock = pokeSnackBlockState.block as PokeSnackBlock
        pokeSnackBlock.eat(level, pokeSnackBlockPos, pokeSnackBlockState, null)
    }

    override fun getMaxPokemonPerChunk(): Float {
        return Cobblemon.config.pokeSnackPokemonPerChunk
    }

    fun getRandomTicksUntilNextSpawn(): Float {
        val biteTimeMultiplier = getBiteTimeMultiplier()
        return (RANDOM_TICKS_BETWEEN_SPAWNS * biteTimeMultiplier).coerceAtLeast(1F)
    }

    fun getBiteTimeMultiplier(): Float {
        val baitEffects = pokeSnackBlockEntity.getBaitEffects()
        val biteTimeEffects = baitEffects.filter { it.type == SpawnBait.Effects.BITE_TIME }
        if (biteTimeEffects.isEmpty()) return 1F

        val biteTimeEffect = biteTimeEffects.random()
        if (Math.random() > biteTimeEffect.chance) {
            return 1F
        }

        return 1F - biteTimeEffect.value.toFloat()
    }
}