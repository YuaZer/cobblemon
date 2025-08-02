/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.server.riding

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.RegistryFriendlyByteBuf

class ServerboundUpdateDriverInputPacket internal constructor(
    val xxa: Float,
    val zza: Float,
    val jumping: Boolean,
    val crouching: Boolean
) : NetworkPacket<ServerboundUpdateDriverInputPacket> {
    override val id = ID
    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeFloat(xxa)
        buffer.writeFloat(zza)
        buffer.writeBoolean(jumping)
        buffer.writeBoolean(crouching)
    }

    companion object {
        val ID = cobblemonResource("c2s_update_driver_input")
        fun decode(buffer: RegistryFriendlyByteBuf): ServerboundUpdateDriverInputPacket {
            val xxa = buffer.readFloat()
            val zza = buffer.readFloat()
            val jumping = buffer.readBoolean()
            val crouching = buffer.readBoolean()
            return ServerboundUpdateDriverInputPacket(xxa, zza, jumping, crouching)
        }
    }
}
