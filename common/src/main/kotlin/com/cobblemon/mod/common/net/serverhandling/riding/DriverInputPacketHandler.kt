/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.serverhandling.riding

import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.duck.ServerPlayerDuck
import com.cobblemon.mod.common.mixin.accessor.ChunkMapAccessor
import com.cobblemon.mod.common.mixin.accessor.TrackedEntityAccessor
import com.cobblemon.mod.common.net.messages.server.riding.ServerboundUpdateDriverInputPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import org.joml.Vector3f
import kotlin.math.sign

object DriverInputPacketHandler : ServerNetworkPacketHandler<ServerboundUpdateDriverInputPacket> {

    override fun handle(
        packet: ServerboundUpdateDriverInputPacket,
        server: MinecraftServer,
        player: ServerPlayer
    ) {
        if (player is ServerPlayerDuck) {
            val vertInput = when {
                packet.jumping-> 1.0f
                packet.crouching -> -1.0f
                else -> 0.0f
            }
            val driverInput = Vector3f(packet.xxa.sign, vertInput, packet.zza.sign)
            player.setDriverInput(driverInput)
        }
    }
}