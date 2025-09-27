/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import com.bedrockk.molang.Expression
import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.api.data.DataRegistry
import com.cobblemon.mod.common.api.molang.ExpressionLike
import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourSettings
import com.cobblemon.mod.common.api.riding.behaviour.types.air.JetSettings
import com.cobblemon.mod.common.net.messages.client.data.RideSettingsSyncPacket
import com.cobblemon.mod.common.util.adapters.ExpressionAdapter
import com.cobblemon.mod.common.util.adapters.ExpressionLikeAdapter
import com.cobblemon.mod.common.util.adapters.FloatNumberRangeAdapter
import com.cobblemon.mod.common.util.cobblemonResource
import com.google.gson.GsonBuilder
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager

object CobblemonRideSettings : DataRegistry {
    override val id: ResourceLocation = cobblemonResource("ride_settings")
    override val type = PackType.SERVER_DATA
    override val observable = SimpleObservable<CobblemonRideSettings>()
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Expression::class.java, ExpressionAdapter)
        .registerTypeAdapter(ExpressionLike::class.java, ExpressionLikeAdapter)
        .registerTypeAdapter(MinMaxBounds.Doubles::class.java, FloatNumberRangeAdapter)
        .create()

    var jet = JetSettings()

    override fun sync(player: ServerPlayer) {
        player.sendPacket(
            RideSettingsSyncPacket(
                jet = jet
            )
        )
    }

    override fun reload(manager: ResourceManager) {
        jet = loadStyle(manager, "jet", JetSettings::class.java)
    }

    private fun <T : RidingBehaviourSettings> loadStyle(manager: ResourceManager, name: String, clazz: Class<T>): T {
        manager.getResourceOrThrow(cobblemonResource("ride_settings/$name.json")).open().use {
            return gson.fromJson(it.reader(), clazz)
        }
    }
}