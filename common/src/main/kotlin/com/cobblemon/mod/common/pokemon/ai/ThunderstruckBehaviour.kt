/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.ai

import com.cobblemon.mod.common.api.molang.ObjectValue

class ThunderstruckBehaviour {
    val rotateAspects = mutableListOf<String>()

    // shorthand of ‘should run special code in thunderHit’
    fun isSpecial(): Boolean = rotateAspects.isNotEmpty()

    @Transient
    val struct = ObjectValue(this).also {
        it.addFunction("has_rotating_aspect") { this.rotateAspects.isNotEmpty() }
    }
}