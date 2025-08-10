/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.ai

import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.api.molang.ObjectValue
import com.cobblemon.mod.common.util.server
import kotlin.math.max
import net.minecraft.world.item.ItemStack

class ItemBehavior {
    private val desiredItems = mutableListOf<ObtainableItem>()
    @Transient
    var highestPriorityItem = 0

    fun getMatchingEntry(stack: ItemStack): ObtainableItem? {
        val registryAccess = server()?.registryAccess() ?: return null
        return if (stack == ItemStack.EMPTY) {
            null
        } else {
            desiredItems.find { it.item?.isItemObtainable(registryAccess, stack) != false }
        }
    }

    @Transient
    val struct = ObjectValue(this).also {
        it.addFunction("add_pickup_item") { params ->
            addPickupItem(params.get<ObjectValue<ObtainableItem>>(0).obj)
        }
    }

    fun getItemPriority(stack: ItemStack): Int {
        return getMatchingEntry(stack)?.pickupPriority ?: 0
    }

    fun getOnUseEffect(stack: ItemStack): ExpressionLike? {
        return getMatchingEntry(stack)?.onUseEffect
    }

    fun addPickupItem(entry: ObtainableItem) {
        desiredItems.add(entry)
        highestPriorityItem = max(entry.pickupPriority, highestPriorityItem)
    }

    fun initialize() {
        highestPriorityItem = desiredItems.maxOfOrNull { it.pickupPriority } ?: 0
    }
}