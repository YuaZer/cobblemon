package com.cobblemon.mod.common.pokemon.ai
import com.cobblemon.mod.common.registry.ItemTagCondition
import com.cobblemon.mod.common.util.asIdentifierDefaultingNamespace
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

    fun getMatchingEntry(stack: ItemStack): ObtainableItem? {
        val item = stack.item
        val itemId = BuiltInRegistries.ITEM.getKey(item)

        if(itemId in itemMap.keys) {
            return itemMap[itemId]
        }
        val tag = tagMap.keys.firstOrNull { stack.`is`(it)}

        if (tag != null) {
            return tagMap[tag]
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
        }
    }
}