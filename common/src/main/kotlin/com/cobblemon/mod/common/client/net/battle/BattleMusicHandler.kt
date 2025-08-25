/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.battle

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.client.sound.BattleMusicController
import com.cobblemon.mod.common.client.sound.instances.BattleMusicInstance
import com.cobblemon.mod.common.net.messages.client.battle.BattleMusicPacket
import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundEvent

/**
 * The handler for [BattleMusicPacket]s. Interfaces with [BattleMusicController] to change battle music.
 *
 * @author Segfault Guy
 * @since April 22nd, 2023
 */
object BattleMusicHandler : ClientNetworkPacketHandler<BattleMusicPacket> {

    override fun handle(packet: BattleMusicPacket, client: Minecraft) {
        val soundManager = client.soundManager
        val loc = packet.music
        val newMusic = loc?.let {
            val event = SoundEvent.createVariableRangeEvent(loc)
            BattleMusicInstance(event, packet.volume, packet.pitch)
        }
        val currMusic = BattleMusicController.music

        if (newMusic?.location == currMusic.location && soundManager.isActive(currMusic) && !currMusic.isFading()) {
            return
        }

        when {
            newMusic == null -> BattleMusicController.endMusic()
            !soundManager.isActive(currMusic) || currMusic.isFading() -> BattleMusicController.initializeMusic(newMusic)
            else -> BattleMusicController.switchMusic(newMusic)
        }
    }
}
