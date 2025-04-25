/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;
import com.cobblemon.mod.common.duck.ChannelDuck;
import com.mojang.blaze3d.audio.Channel;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Channel.class)
public abstract class ChannelMixin implements ChannelDuck {

    @Shadow
    private int source;

    // Id of filter applied when sound is obstructed
    private int cobblemon$lowPassFilterId = 0;

    @Override
    public void cobblemon$applyLowPassFilter(float gain, float hfGain) {
        // If a filter already exists then return
        if (cobblemon$lowPassFilterId != 0) return;
        cobblemon$lowPassFilterId = EXTEfx.alGenFilters();

        if (EXTEfx.alIsFilter(cobblemon$lowPassFilterId)) {
            EXTEfx.alFilteri(cobblemon$lowPassFilterId, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
            EXTEfx.alFilterf(cobblemon$lowPassFilterId, EXTEfx.AL_LOWPASS_GAIN, gain);
            EXTEfx.alFilterf(cobblemon$lowPassFilterId, EXTEfx.AL_LOWPASS_GAINHF, hfGain);

            AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, cobblemon$lowPassFilterId);
        } else {
            System.err.println("Failed to create OpenAL low-pass filter");
        }
    }

    @Override
    public void cobblemon$clearFilters() {
        AL10.alSourcei(source, EXTEfx.AL_DIRECT_FILTER, 0);

        if (EXTEfx.alIsFilter(cobblemon$lowPassFilterId)) {
            EXTEfx.alDeleteFilters(cobblemon$lowPassFilterId);
            cobblemon$lowPassFilterId = 0;
        }
    }

//    @Override
//    public void cobblemon$exponentialAttenuation(float rolloff, float refDistance, float maxDistance) {
//        AL10.alSourcei(this.source, AL10.AL_DISTANCE_MODEL, 53251);
//        AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, rolloff);
//        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 0.0F);
//        AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, maxDistance);
//    }



}
