/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.stats

import net.minecraft.resources.ResourceLocation

object CobblemonStats {
    val stats = mutableMapOf<String,  ResourceLocation>()

    var CAPTURED = ResourceLocation.fromNamespaceAndPath("cobblemon", "captured") //makeCustomStat("captured", StatFormatter.DEFAULT)
    val SHINIES_CAPTURED = ResourceLocation.fromNamespaceAndPath("cobblemon", "shinies_captured") //makeCustomStat("shinies_captured", StatFormatter.DEFAULT)
    val RELEASED = ResourceLocation.fromNamespaceAndPath("cobblemon", "released") //makeCustomStat("released", StatFormatter.DEFAULT)
    val EVOLVED = ResourceLocation.fromNamespaceAndPath("cobblemon", "evolved") //makeCustomStat("evolved", StatFormatter.DEFAULT)
    val LEVEL_UP = ResourceLocation.fromNamespaceAndPath("cobblemon", "level_up") //makeCustomStat("level_up", StatFormatter.DEFAULT)
    val BATTLES_WON = ResourceLocation.fromNamespaceAndPath("cobblemon", "battles_won") //makeCustomStat("battles_won", StatFormatter.DEFAULT)
    val BATTLES_LOST = ResourceLocation.fromNamespaceAndPath("cobblemon", "battles_lost") //makeCustomStat("battles_lost", StatFormatter.DEFAULT)
    val BATTLES_FLED = ResourceLocation.fromNamespaceAndPath("cobblemon", "battles_fled") //makeCustomStat("battles_fled", StatFormatter.DEFAULT)
    val BATTLES_TOTAL = ResourceLocation.fromNamespaceAndPath("cobblemon", "battles_total") //makeCustomStat("battles_total", StatFormatter.DEFAULT)
    val DEX_ENTRIES = ResourceLocation.fromNamespaceAndPath("cobblemon", "dex_entries") //makeCustomStat("dex_entries", StatFormatter.DEFAULT)
    val EGGS_COLLECTED = ResourceLocation.fromNamespaceAndPath("cobblemon", "eggs_collected") //makeCustomStat("eggs_collected", StatFormatter.DEFAULT)
    val EGGS_HATCHED = ResourceLocation.fromNamespaceAndPath("cobblemon", "eggs_hatched") //makeCustomStat("eggs_hatched", StatFormatter.DEFAULT)
    val TRADED = ResourceLocation.fromNamespaceAndPath("cobblemon", "traded") //makeCustomStat("traded", StatFormatter.DEFAULT)
    val FOSSILS_REVIVED = ResourceLocation.fromNamespaceAndPath("cobblemon", "fossils_revived") //makeCustomStat("fossils_revived", StatFormatter.DEFAULT)
    val POKEMON_INTERACTED_WITH = ResourceLocation.fromNamespaceAndPath("cobblemon", "pokemon_interacted_with") //makeCustomStat("pokemon_interacted_with", StatFormatter.DEFAULT)
    //TODO block stats (interact, gimmi), riding (styles?)

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
        stats["pokemon_interacted_with"] = POKEMON_INTERACTED_WITH
    }
}
