package com.cobblemon.mod.common.pokemon.ai

import com.cobblemon.mod.common.registry.ItemTagCondition
import net.minecraft.resources.ResourceLocation


class ObtainableItem(
    val item: ResourceLocation? = null,
    val tag: String? = null,
    val pickupPriority: Int = 0,
    val isFood: Boolean = false,
    val fullnessValue: Int = 0,
    val returnItem: ResourceLocation? = null,
    val onUseEffect: String? = null,
    val onPickUpEffect: String = "",
    val onDropEffect: String = ""
) {

}