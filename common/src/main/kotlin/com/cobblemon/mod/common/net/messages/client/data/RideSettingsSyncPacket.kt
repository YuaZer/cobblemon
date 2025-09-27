/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.data

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.api.riding.behaviour.types.air.JetSettings
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.RegistryFriendlyByteBuf

/**
 * Synchronizes the [com.cobblemon.mod.common.CobblemonRideSettings] registry.
 */
class RideSettingsSyncPacket(
    val jet: JetSettings
) : NetworkPacket<RideSettingsSyncPacket> {
    override val id = ID

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        jet.encode(buffer)
    }

    companion object {
        val ID = cobblemonResource("mechanics_sync")
        fun decode(buffer: RegistryFriendlyByteBuf): RideSettingsSyncPacket {
            val jet = JetSettings()
            jet.decode(buffer)
            return RideSettingsSyncPacket(jet)
        }
    }
}