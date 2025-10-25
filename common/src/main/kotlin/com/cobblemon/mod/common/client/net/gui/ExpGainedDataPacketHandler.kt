package com.cobblemon.mod.common.client.net.gui

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.client.gui.PartyOverlayDataControl
import com.cobblemon.mod.common.net.messages.client.ui.ExpGainedDataPacket
import net.minecraft.client.Minecraft

object ExpGainedDataPacketHandler: ClientNetworkPacketHandler<ExpGainedDataPacket> {
    override fun handle(packet: ExpGainedDataPacket, client: Minecraft) {
        PartyOverlayDataControl.pokemonGainedExp(
            packet.pokemonUUID,
            packet.oldLevel,
            packet.expGained,
            packet.countOfMovesLearned
        )
    }
}