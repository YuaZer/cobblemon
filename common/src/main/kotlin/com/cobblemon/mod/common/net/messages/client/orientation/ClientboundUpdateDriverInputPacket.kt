/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.orientation

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.net.messages.client.pokemon.update.ClientboundUpdateRidingStatePacket.Companion.ID
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.readMatrix3f
import com.cobblemon.mod.common.util.writeMatrix3f
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.joml.Matrix3f

/**
 * Packet sent from the server to update the current input from
 * the driver of a ridden pokemon. This data is needed for client
 * side animation data updates found in RidingAnimationData
 *
 * @author Jackowes
 * @since August 1st, 2025
 */

class ClientboundUpdateDriverInputPacket internal constructor(
    val xxa: Float,
    val zza: Float,
    val jumping: Boolean,
    val crouching: Boolean,
    val entityId: Int
) : NetworkPacket<ClientboundUpdateDriverInputPacket> {
    override val id = ID
    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeVarInt(entityId)
        buffer.writeFloat(xxa)
        buffer.writeFloat(zza)
        buffer.writeBoolean(jumping)
        buffer.writeBoolean(crouching)
    }

    companion object {
        val ID = cobblemonResource("s2c_update_driver_input")
        fun decode(buffer: RegistryFriendlyByteBuf): ClientboundUpdateDriverInputPacket {
            val entityId = buffer.readVarInt()
            val xxa = buffer.readFloat()
            val zza = buffer.readFloat()
            val jumping = buffer.readBoolean()
            val crouching = buffer.readBoolean()
            return ClientboundUpdateDriverInputPacket(xxa, zza, jumping, crouching, entityId)
        }
    }
}
