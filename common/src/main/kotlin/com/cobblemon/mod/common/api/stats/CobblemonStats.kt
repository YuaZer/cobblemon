/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.stats

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.stats.StatFormatter
import net.minecraft.stats.StatType
import net.minecraft.stats.Stats

object CobblemonStats {

    val CAPTURED = makeCustomStat("captured", StatFormatter.DEFAULT)
    val SHINIES_CAPTURED = makeCustomStat("shinies_captured", StatFormatter.DEFAULT)
    val RELEASED = makeCustomStat("released", StatFormatter.DEFAULT)
    val EVOLVED = makeCustomStat("evolved", StatFormatter.DEFAULT)
    val LEVEL_UP = makeCustomStat("level_up", StatFormatter.DEFAULT)
    val BATTLES_WON = makeCustomStat("battles_won", StatFormatter.DEFAULT)
    val BATTLES_LOST = makeCustomStat("battles_lost", StatFormatter.DEFAULT)
    val BATTLES_FLED = makeCustomStat("battles_fled", StatFormatter.DEFAULT)
    val BATTLES_TOTAL = makeCustomStat("battles_total", StatFormatter.DEFAULT)
    val DEX_ENTRIES = makeCustomStat("dex_entries", StatFormatter.DEFAULT)
    val EGGS_COLLECTED = makeCustomStat("eggs_collected", StatFormatter.DEFAULT)
    val EGGS_HATCHED = makeCustomStat("eggs_hatched", StatFormatter.DEFAULT)
    val TRADED = makeCustomStat("traded", StatFormatter.DEFAULT)
    val FOSSILS_REVIVED = makeCustomStat("fossils_revived", StatFormatter.DEFAULT)
    //TODO block stats (interact, fossil revival, gimmi), riding (styles?)

    private fun makeCustomStat(key: String, formatter: StatFormatter): ResourceLocation {
        val resourceLocation = ResourceLocation.fromNamespaceAndPath("cobblemon", key)
        Registry.register<ResourceLocation?>(BuiltInRegistries.CUSTOM_STAT, key, resourceLocation)
        Stats.CUSTOM.get(resourceLocation, formatter)
        return resourceLocation
    }

    private fun <T> makeRegistryStatType(key: String, registry: Registry<T>): StatType<T> {
        val component: Component = Component.translatable("stat_type.cobblemon.$key")
        return Registry.register<StatType<T>>(
            BuiltInRegistries.STAT_TYPE,
            key,
            StatType<T>(registry, component)
        ) as StatType<T>
    }
}
