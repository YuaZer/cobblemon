/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mechanics

import com.cobblemon.mod.common.api.apricorn.Apricorn
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.client.pot.CookingQuality

/**
 * Mechanic to hold various properties that motivate aprijuice as a mechanic.
 *
 * @author Hiroku
 * @since November 7th, 2025
 */
class AprijuicesMechanic {
    /** The points that apply to different riding stats, based on the apricorn used for the aprijuice. */
    val apricornStatEffects = mutableMapOf<Apricorn, Map<RidingStat, Int>>()
    /** Maps flavour values to stat points for riding stats. Aprijuice finds the highest threshold it meets. */
    val statPointFlavourThresholds = mutableMapOf<Int, Int>()
    /** Maps stat points for riding stats to what cooking quality that represents. It's for tooltips. */
    val cookingQualityPointThresholds = mutableMapOf<Int, CookingQuality>()
}