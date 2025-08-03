/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.ai

import net.minecraft.resources.ResourceLocation


class ObtainableItem(
    val item: ResourceLocation? = null,
    val tag: String? = null,
    val itemQuery: String? = null, // molang item condition (Expression, Item) => Boolean
    val pickupPriority: Int = 0, // Will desire the highest value, a pokemon will immediately drop an item with negative priority
    val fullnessValue: Int = 0,
    val returnItem: ResourceLocation? = null,
    val onUseEffect: String? = null, // Molang Expression that plays against the entity when the item is consumed
) {

}