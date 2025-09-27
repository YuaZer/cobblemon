/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.data

import com.cobblemon.mod.common.CobblemonRideSettings
import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.net.messages.client.data.RideSettingsSyncPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object RideSettingsSyncHandler : ServerNetworkPacketHandler<RideSettingsSyncPacket> {
    override fun handle(packet: RideSettingsSyncPacket, server: MinecraftServer, player: ServerPlayer) {
        CobblemonRideSettings.bird = packet.bird
        CobblemonRideSettings.glider = packet.glider
        CobblemonRideSettings.helicopter = packet.helicopter
        CobblemonRideSettings.hover = packet.hover
        CobblemonRideSettings.jet = packet.jet
        CobblemonRideSettings.rocket = packet.rocket

        CobblemonRideSettings.horse = packet.horse
        CobblemonRideSettings.minekart = packet.minekart
        CobblemonRideSettings.vehicle = packet.vehicle

        CobblemonRideSettings.boat = packet.boat
        CobblemonRideSettings.burst = packet.burst
        CobblemonRideSettings.dolphin = packet.dolphin
    }
}