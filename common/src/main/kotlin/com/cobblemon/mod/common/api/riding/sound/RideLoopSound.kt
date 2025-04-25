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
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
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

    var shouldMuffle: Boolean = false
    var isPassenger: Boolean = false

    init {
        this.looping = true
        this.delay = 0
        this.volume = 1.0f
        this.shouldMuffle = false
        this.attenuation = SoundInstance.Attenuation.NONE
        this.isPassenger = Minecraft.getInstance().player?.id == ride.controllingPassenger?.id
        if (this.isPassenger) this.relative = true
    }

    override fun tick() {
        this.volume = ((volumeExpr?.let { ride.runtime.resolveDouble(it) }) ?: 1.0).toFloat()
        this.pitch = ((pitchExpr?.let { ride.runtime.resolveDouble(it) }) ?: 1.0).toFloat()

        // TODO: Stop raycasting if the ride is out of attenuation distance.
        if (!this.isPassenger) {
            this.setPos()

            // Attenuate due to distance
            this.volume *= attenuation()

            // Calculate influence due to the doppler effect
            this.pitch *= calcDopplerInfluence().toFloat()

            // Calculate muffling due to occlusion
            val soundOc = soundOcclusion()
            if (soundOc < 1.0f) {
                this.shouldMuffle = true
                this.volume *= 0.7f
            } else {
                this.shouldMuffle = false
            }
        }
    }

    fun calcDopplerInfluence(): Double {
        val rideVel = ride.getVelocity()

        // TODO: Expand this to multiple players
        val listener = Minecraft.getInstance().player ?: return 1.0
        val listenerVel = listener.position().subtract(Vec3(listener.xOld, listener.yOld, listener.zOld))

        // Add the speed at which the player is traveling towards the ride and the speed at which the ride is traveling
        // towards the player

        // Tweaked constant based off speed of sound in room temp air. Exaggerated for a better effect in minecraft
        val waveSpeed = 343.0f / 150.0f

        // listener->ride los velocity
        val lineOfSightToRide = ride.position().subtract(listener.position()).normalize()
        //val lineOfSightToPlayer = listener.position().subtract(ride.position()).normalize()

        // Speed of the observer towards the sound source
        val velObserver = listenerVel.dot(lineOfSightToRide)
        // Speed of the sound source towards the observer
        val velSrc = rideVel.dot(lineOfSightToRide.scale(-1.0))

        // Use the doppler effect equation to determine the pitch change
        val pitchFactor = (waveSpeed + velObserver) / (waveSpeed - velSrc)

        return pitchFactor

    }

    fun soundOcclusion(): Double {
        val listener = Minecraft.getInstance().player ?: return 1.0
        val level = listener.level()
        val maxAttenDist = this.sound.attenuationDistance
        val minMuffle = 0.3

        var totalMuffle = 0.0
        var totalRays = 0

        for (pitch in listOf(-2.0, 0.0, 2.0)) {
            for (yaw in listOf(-2.0, 0.0, 2.0)) {
                totalRays += 1
                val hit = level.clip(
                    ClipContext(
                        listener.eyePosition,
                        ride.position(),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        listener
                    )
                )

                if(hit.type != HitResult.Type.MISS) {
                    //Calculate the distance the hit was away from the listener
                    val hitDist = hit.location.distanceTo(listener.eyePosition)

                    //Calculate the influence based on distance of this particular ray hit
                    val hitMuffle = Mth.lerp((1 - (hitDist / maxAttenDist)), 1.0, minMuffle)
                    totalMuffle += hitMuffle
                }
            }
        }


        return (totalMuffle / totalRays.toDouble())
    }


    fun attenuation(): Float {
        val listener = Minecraft.getInstance().player ?: return 1.0f
        val soundDistance = (ride.position().subtract(listener.eyePosition)).length()
        val maxAttenDistance = this.sound.attenuationDistance

        return (1 - (soundDistance / maxAttenDistance).pow(2)).toFloat()
    }

    fun setPos() {
        this.x = ride.x
        this.y = ride.y
        this.z = ride.z
    }

    fun stopSound() {
        Minecraft.getInstance().soundManager.stop(this)
    }
}