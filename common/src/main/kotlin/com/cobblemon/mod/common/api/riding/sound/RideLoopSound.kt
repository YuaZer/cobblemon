/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.riding.sound

import com.bedrockk.molang.Expression
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.resolveDouble
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import kotlin.math.min
import kotlin.math.pow


/**
 * Class to handle looping ride sounds for clients.
 *
 * @author Jackowes
 * @since April 21st, 2025
 */

class RideLoopSound(val ride: PokemonEntity, sound: SoundEvent, val volumeExpr: Expression?, val pitchExpr: Expression?) :
        AbstractTickableSoundInstance(sound, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom()) {

    init {
        this.looping = true
        this.delay = 0
        this.volume = 1.0f
    }

    override fun tick() {
        this.setPos()
        this.volume = ((volumeExpr?.let { ride.runtime.resolveDouble(it) }) ?: 1.0).toFloat()
        this.pitch = ((pitchExpr?.let { ride.runtime.resolveDouble(it) }) ?: 1.0).toFloat()
        this.attenuation
    }

//        this.x = ride.x
//        this.y = ride.y
//        this.z = ride.z
//
//        this.volume = min(ride.deltaMovement.length() / 1.5f, 1.0).pow(2).toFloat()
//
//        if (this.volume > 0.8f) {
//            this.pitch = 1.0f + (this.volume - 0.8f)
//        } else {
//            this.pitch = 1.0f
//        }
//
//        this.volume *= 0.5f
//    }

    fun setPos() {
        this.x = ride.x
        this.y = ride.y
        this.z = ride.z
    }

    fun stopSound() {
        Minecraft.getInstance().soundManager.stop(this)
    }
}