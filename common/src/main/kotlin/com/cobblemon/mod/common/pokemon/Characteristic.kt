/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon

import com.cobblemon.mod.common.api.pokemon.stats.Stat

const val CHARACTERISTIC_MODULUS: Int = 5

data class Characteristic(val relevantStat: Stat, val mod: Int) {
    // Returns translation keys in the shape of "cobblemon.characteristic.attack.1.desc".
    // mod is a number between 0 and 4, inclusive.
    fun getTranslationKey(): String =
        "${relevantStat.identifier.namespace}.characteristic.${relevantStat.identifier.path}.${mod}.desc"
}
