package com.cobblemon.mod.common.net.messages.client.ui

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.RegistryFriendlyByteBuf
import java.util.UUID

class ExpGainedDataPacket(
    val pokemonUUID: UUID,
    val oldLevel: Int?,
    val expGained: Int,
    val countOfMovesLearned: Int
): NetworkPacket<ExpGainedDataPacket> {
    override val id = ID

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeUUID(pokemonUUID)
        buffer.writeNullable(oldLevel) { pb, v -> pb.writeInt(v) }
        buffer.writeInt(expGained)
        buffer.writeInt(countOfMovesLearned)
    }

    companion object {
        val ID = cobblemonResource("exp_gained_data")
        fun decode(buffer: RegistryFriendlyByteBuf) = ExpGainedDataPacket(
            buffer.readUUID(),
            buffer.readNullable { buffer.readInt() },
            buffer.readInt(),
            buffer.readInt()
        )
    }
}