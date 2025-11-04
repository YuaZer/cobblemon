/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.components

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.FastColor
import net.minecraft.world.item.DyeColor

/**
 * This component contains a list of [DyeColor] picked up from cooking.
 *
 * @author Hiroku
 * @since March 20th, 2025
 */
class FoodColourComponent(
    val colours: List<DyeColor>
) {
    companion object {
        val CODEC: Codec<FoodColourComponent> = DyeColor.CODEC.listOf().xmap(::FoodColourComponent, FoodColourComponent::colours)
        val PACKET_CODEC: StreamCodec<ByteBuf, FoodColourComponent> = ByteBufCodecs.fromCodec(CODEC)

        private val COLORS = mapOf<DyeColor, Int>(
            DyeColor.RED to FastColor.ARGB32.color(255, 235, 64, 52),
            DyeColor.YELLOW to FastColor.ARGB32.color(255, 255, 223, 79),
            DyeColor.GREEN to FastColor.ARGB32.color(255, 93, 201, 98),
            DyeColor.BLUE to FastColor.ARGB32.color(255, 70, 162, 255),
            DyeColor.PINK to FastColor.ARGB32.color(255, 255, 149, 192),
            DyeColor.CYAN to FastColor.ARGB32.color(255, 102, 244, 255),
            DyeColor.PURPLE to FastColor.ARGB32.color(255, 178, 102, 255),
            DyeColor.ORANGE to FastColor.ARGB32.color(255, 255, 159, 41),
            DyeColor.LIME to FastColor.ARGB32.color(255, 173, 255, 47),
            DyeColor.LIGHT_BLUE to FastColor.ARGB32.color(255, 144, 205, 255),
            DyeColor.MAGENTA to FastColor.ARGB32.color(255, 255, 102, 255),
            DyeColor.WHITE to FastColor.ARGB32.color(255, 255, 255, 255),
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is FoodColourComponent &&
                colours.size == other.colours.size &&
                colours.mapIndexed { index, value -> value == other.colours[index] }.all { it }
    }

    fun getColoursAsARGB(): List<Int> {
        return colours.map { dye -> COLORS[dye] ?: COLORS[DyeColor.WHITE]!! }
    }

    override fun hashCode() = colours.hashCode()
}