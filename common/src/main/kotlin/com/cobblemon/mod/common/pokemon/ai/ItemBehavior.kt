package com.cobblemon.mod.common.pokemon.ai
import com.bedrockk.molang.Expression
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.util.asExpression
import com.cobblemon.mod.common.util.asIdentifierDefaultingNamespace
import com.cobblemon.mod.common.util.resolveBoolean
import com.cobblemon.mod.common.util.resolveInt
import com.cobblemon.mod.common.util.server
import com.cobblemon.mod.common.util.withQueryValue
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import kotlin.collections.mutableListOf

class ItemBehavior {
    private val desiredItems = mutableListOf<ObtainableItem>()
    private val itemMap = mutableMapOf<ResourceLocation, ObtainableItem>()
    private val tagMap = mutableMapOf<TagKey<Item>, ObtainableItem>()
    private val queryList = mutableMapOf<Expression, ObtainableItem>()
    fun getMatchingEntry(stack: ItemStack): ObtainableItem? {
        if (stack == ItemStack.EMPTY) {
            return null
        }
        val item = stack.item
        val itemId = BuiltInRegistries.ITEM.getKey(item)

        if(itemId in itemMap.keys) {
            return itemMap[itemId]
        }
        val tag = tagMap.keys.firstOrNull { stack.`is`(it)}

        if (tag != null) {
            return tagMap[tag]
        }
        // run queries on item
        val registryAccess = server()?.registryAccess()
        if (registryAccess != null) {
            val runtime = MoLangRuntime().setup()
            runtime.withQueryValue("item", stack.asMoLangValue(registryAccess))
            for (query in queryList.keys) {
                val match = runtime.resolveBoolean(query)
                if (match) {
                    return queryList[query]
                }
            }

        }

        return null
    }

    fun getItemPriority(stack: ItemStack): Int {
        return getMatchingEntry(stack)?.pickupPriority ?: 0
    }

    fun getOnUseEffect(stack: ItemStack): String? {
        return getMatchingEntry(stack)?.onUseEffect
    }

    fun initialize() {
        desiredItems.forEach { entry ->
            if (entry.item != null) {
                itemMap[entry.item] = entry
            }
            if (entry.tag != null) {
                val tag = TagKey.create(Registries.ITEM, entry.tag.replace("#", "").asIdentifierDefaultingNamespace())
                tag.let { tagMap[tag] = entry }
            }
            if (entry.itemQuery != null) {
                queryList[entry.itemQuery.asExpression()] = entry
            }
        }
    }
}