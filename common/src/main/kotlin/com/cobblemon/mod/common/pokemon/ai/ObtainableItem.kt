package com.cobblemon.mod.common.pokemon.ai

import net.minecraft.resources.ResourceLocation


class ObtainableItem(
    val item: ResourceLocation? = null,
    val tag: String? = null,
    val itemQuery: String? = null, // molang item condition (Expression, Item) => Boolean
    val pickupPriority: Int = 0, // Will desire the hight value, a pokemon will immediately drop an item with negative priority
    val isFood: Boolean = false, // plays eating sounds/particles if true
    val fullnessValue: Int = 0,
    val returnItem: ResourceLocation? = null,
    val onUseEffect: String? = null, // Molang Expression that plays against the entity when the item is confused
    val onPickUpEffect: String = "",
    val onDropEffect: String = ""
) {

}