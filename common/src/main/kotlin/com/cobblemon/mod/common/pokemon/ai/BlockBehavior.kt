/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.ai

import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.api.molang.ObjectValue
import com.cobblemon.mod.common.util.getBooleanOrNull

class BlockBehavior {
    var immuneToSweetBerryBushBlock = false
    var canStandOnPowderSnow = false

    @Transient
    val struct = ObjectValue(this).also {
        it.addFunction("immune_to_sweet_berry_bush_block") { DoubleValue(immuneToSweetBerryBushBlock) }
        it.addFunction("can_stand_on_powder_snow") { DoubleValue(canStandOnPowderSnow) }
        it.addFunction("set_immune_to_sweet_berry_bush_block") { params -> immuneToSweetBerryBushBlock = params.getBooleanOrNull(0) ?: true }
        it.addFunction("set_can_stand_on_powder_snow") { params -> canStandOnPowderSnow = params.getBooleanOrNull(0) ?: false }
    }
}