/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.stats

import com.cobblemon.mod.common.Cobblemon
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.stats.StatFormatter

object CobblemonStats {
    val stats = mutableMapOf<String, CobblemonStat>()

    var CAPTURED: CobblemonStat = CobblemonStat("captured")
    val SHINIES_CAPTURED: CobblemonStat = CobblemonStat("shinies_captured")
    val RELEASED: CobblemonStat = CobblemonStat("released")
    val EVOLVED: CobblemonStat = CobblemonStat("evolved")
    val LEVEL_UP: CobblemonStat = CobblemonStat("level_up")
    val BATTLES_WON: CobblemonStat = CobblemonStat("battles_won")
    val BATTLES_LOST: CobblemonStat = CobblemonStat("battles_lost")
    val BATTLES_FLED: CobblemonStat = CobblemonStat("battles_fled")
    val BATTLES_TOTAL: CobblemonStat = CobblemonStat("battles_total")
    val DEX_ENTRIES: CobblemonStat = CobblemonStat("dex_entries")
    val EGGS_COLLECTED: CobblemonStat = CobblemonStat("eggs_collected")
    val EGGS_HATCHED: CobblemonStat = CobblemonStat("eggs_hatched")
    val TRADED: CobblemonStat = CobblemonStat("traded")
    val FOSSILS_REVIVED: CobblemonStat = CobblemonStat("fossils_revived")
    val TIMES_RIDDEN: CobblemonStat = CobblemonStat("times_ridden")
    val ROD_CASTS: CobblemonStat = CobblemonStat("rod_casts")
    val REEL_INS: CobblemonStat = CobblemonStat("reel_ins")
    val POKEMON_INTERACTED_WITH: CobblemonStat = CobblemonStat("pokemon_interacted_with")
    val RIDING_LAND: CobblemonStat = CobblemonStat("riding_land", StatFormatter.DISTANCE)
    val RIDING_AIR: CobblemonStat = CobblemonStat("riding_air", StatFormatter.DISTANCE)
    val RIDING_LIQUID: CobblemonStat = CobblemonStat("riding_liquid", StatFormatter.DISTANCE)

    //TODO block, stats (interact, gimmi)

    fun registerStats() {
        stats["captured"] = CAPTURED
        stats["shinies_captured"] = SHINIES_CAPTURED
        stats["released"] = RELEASED
        stats["evolved"] = EVOLVED
        stats["level_up"] = LEVEL_UP
        stats["battles_won"] = BATTLES_WON
        stats["battles_lost"] = BATTLES_LOST
        stats["battles_fled"] = BATTLES_FLED
        stats["battles_total"] = BATTLES_TOTAL
        stats["dex_entries"] = DEX_ENTRIES
        stats["eggs_collected"] = EGGS_COLLECTED
        stats["eggs_hatched"] = EGGS_HATCHED
        stats["traded"] = TRADED
        stats["fossils_revived"] = FOSSILS_REVIVED
        stats["times_ridden"] = TIMES_RIDDEN
        stats["rod_casts"] = ROD_CASTS
        stats["reel_ins"] = REEL_INS
        stats["pokemon_interacted_with"] = POKEMON_INTERACTED_WITH
        stats["riding_land"] = RIDING_LAND
        stats["riding_air"] = RIDING_AIR
        stats["riding_liquid"] = RIDING_LIQUID
    }

    fun getStat(cobblemonStat: CobblemonStat) : ResourceLocation {
        val resourceLocation = cobblemonStat.resourceLocation
        val stat = BuiltInRegistries.CUSTOM_STAT.get(resourceLocation)
        if (stat == null) {
            Cobblemon.LOGGER.error("Could not find stat with id {}", resourceLocation)
        }
        return stat ?: throw NullPointerException("Could not find stat with id $resourceLocation")
    }

    data class CobblemonStat(val path: String, val formatter: StatFormatter = StatFormatter.DEFAULT) {
        val resourceLocation: ResourceLocation = ResourceLocation.fromNamespaceAndPath("cobblemon", path);
    }
}
