/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.riding.sound;

import com.bedrockk.molang.Expression
import com.cobblemon.mod.common.api.net.Encodable
import com.cobblemon.mod.common.util.asExpression
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.readExpression
import com.cobblemon.mod.common.util.writeExpression
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation;

/**
 * Class to store data for sounds played during riding.
 * The ride key
 *
 * @author Jackowes
 * @since April 26th, 2025
 */
data class RideSoundSettings(
    val soundLocation: ResourceLocation,
    val volumeExpr: Expression = "1.0".asExpression(),
    val pitchExpr: Expression = "1.0".asExpression(),
    val playForNonPassengers: Boolean = true,
    val muffleEnabled: Boolean = true
): Encodable {

    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeResourceLocation(soundLocation)
        buffer.writeExpression(volumeExpr)
        buffer.writeExpression(pitchExpr)
        buffer.writeBoolean(playForNonPassengers)
        buffer.writeBoolean(muffleEnabled)
    }

    companion object {
        fun decode(buffer: RegistryFriendlyByteBuf): RideSoundSettings {

            // Try to read if available, fallback to default otherwise
            val volumeExpr = if (buffer.isReadable) buffer.readExpression() else "math.pow(math.min(q.ride_velocity() / 1.5, 1.0),2)".asExpression()
            val pitchExpr = if (buffer.isReadable) buffer.readExpression() else "math.max(1.0 ,0.2 + math.pow(math.min(q.ride_velocity() / 1.5, 1.0),2))".asExpression()
            val playForNonPassengers = if (buffer.isReadable) buffer.readBoolean() else true
            val muffleEnabled = if (buffer.isReadable) buffer.readBoolean() else true

            return RideSoundSettings(
                buffer.readResourceLocation(),
                volumeExpr,
                pitchExpr,
                playForNonPassengers,
                muffleEnabled
            )
        }
    }
}

class RideSoundSettingsList(var sounds: List<RideSoundSettings> = mutableListOf()) {

    fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeCollection(sounds) { _, sound -> sound.encode(buffer) }
    }

    companion object {
        fun decode(buffer: RegistryFriendlyByteBuf): RideSoundSettingsList {
            val list = RideSoundSettingsList()
            list.sounds = buffer.readList { RideSoundSettings.decode(buffer) }.toMutableList()
            return list
        }
    }
}
