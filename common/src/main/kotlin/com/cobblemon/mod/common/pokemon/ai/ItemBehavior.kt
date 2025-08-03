/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon.ai

import com.bedrockk.molang.Expression
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.api.molang.ObjectValue
import com.cobblemon.mod.common.util.asExpression
import com.cobblemon.mod.common.util.asIdentifierDefaultingNamespace
import com.cobblemon.mod.common.util.resolveBoolean
import com.cobblemon.mod.common.util.server
import com.cobblemon.mod.common.util.withQueryValue
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import kotlin.collections.mutableListOf
import kotlin.collections.set
import kotlin.math.max

class ItemBehavior {
    private val desiredItems = mutableListOf<ObtainableItem>()
    @Transient
    private val itemMap = mutableMapOf<ResourceLocation, ObtainableItem>()
    @Transient
    private val tagMap = mutableMapOf<TagKey<Item>, ObtainableItem>()
    @Transient
    private val queryMap = LinkedHashMap<Expression, ObtainableItem>() // used LinkedHashMap for deterministic key ordering when iterating through molang queries
    @Transient
    var highestPriorityItem = 0

    fun getMatchingEntry(stack: ItemStack): ObtainableItem? {
        if (stack == ItemStack.EMPTY) {
            return null
        }

        val item = stack.item
        val itemId = BuiltInRegistries.ITEM.getKey(item)

        // Search items first
        if(itemId in itemMap.keys) {
            return itemMap[itemId]
        }

        // Tags second
        val tag = tagMap.keys.firstOrNull { stack.`is`(it)}
        if (tag != null) {
            return tagMap[tag]
        }

        // Queries last
        val registryAccess = server()?.registryAccess()
        if (registryAccess != null) {
            val runtime = MoLangRuntime().setup()
            runtime.withQueryValue("item", stack.asMoLangValue(registryAccess))
            for (query in queryMap.keys) {
                val match = runtime.resolveBoolean(query)
                if (match) {
                    return queryMap[query]
                }
            }

        }
        return null
    }

    @Transient
    val struct = ObjectValue(this).also {
        it.addFunction("add_pickup_item") { params ->
            val npcId = params.get<ObjectValue<ObtainableItem>>(0) // (params.get<MoValue>(0) as? ObjectValue<ObtainableItem>)
            val pickItem = npcId?.obj
            if (pickItem != null) {
                addPickupItem(pickItem)
            }
        }
        it.addFunction("create_pickup_item") { params ->
            val itemName = params.getString(0)
            val tagName = params.getString(1)
            val itemQuery = params.getString(2)
            val returnItemName = params.getString(5)
            val onUseEffect = params.getString(6)
            val pickupItem = ObtainableItem(
                item = if (itemName.isNotEmpty()) ResourceLocation.parse(itemName) else null,
                tag = tagName.ifEmpty { null },
                itemQuery = itemQuery.ifEmpty { null },
                pickupPriority = params.getInt(3),
                fullnessValue = params.getInt(4),
                returnItem = if (returnItemName.isNotEmpty()) ResourceLocation.parse(returnItemName) else null,
                onUseEffect = onUseEffect.ifEmpty { null }
            )
            ObjectValue(pickupItem)
        }
    }

    fun getItemPriority(stack: ItemStack): Int {
        return getMatchingEntry(stack)?.pickupPriority ?: 0
    }

    fun getOnUseEffect(stack: ItemStack): String? {
        return getMatchingEntry(stack)?.onUseEffect
    }

    fun addPickupItem(entry: ObtainableItem) {
        if (entry.item != null) {
            itemMap[entry.item] = entry
        }
        if (entry.tag != null) {
            val tag = TagKey.create(Registries.ITEM, entry.tag.replace("#", "").asIdentifierDefaultingNamespace())
            tag.let { tagMap[tag] = entry }
        }
        if (entry.itemQuery != null) {
            queryMap[entry.itemQuery.asExpression()] = entry
        }
        highestPriorityItem = max(entry.pickupPriority, highestPriorityItem)
    }

    fun initialize() {
        desiredItems.forEach { entry ->
            addPickupItem(entry)
        }
    }
}