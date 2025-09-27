/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.client.data

import com.cobblemon.mod.common.CobblemonMechanics
import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.mechanics.RidingMechanic
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.readString
import com.cobblemon.mod.common.util.writeString
import com.google.gson.JsonElement
import net.minecraft.network.RegistryFriendlyByteBuf

/**
 * Synchronizes whatever elements in [com.cobblemon.mod.common.CobblemonMechanics] need
 * to be synced to the client.
 */
class MechanicsSyncPacket(
    val riding: JsonElement
) : NetworkPacket<MechanicsSyncPacket> {
    override val id = ID

    constructor(riding: RidingMechanic): this(CobblemonMechanics.gson.toJsonTree(riding))

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeString(CobblemonMechanics.gson.toJson(riding))
    }

    companion object {
        val ID = cobblemonResource("mechanics_sync")
        fun decode(buffer: RegistryFriendlyByteBuf): MechanicsSyncPacket {
            return MechanicsSyncPacket(CobblemonMechanics.gson.fromJson(buffer.readString(), JsonElement::class.java))
        }
    }
}