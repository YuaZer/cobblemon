/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.riding.Rideable;
import com.cobblemon.mod.common.client.MountedPokemonAnimationRenderController;
import com.cobblemon.mod.common.client.RidingCameraInterface;
import com.cobblemon.mod.common.client.render.camera.MountedCameraRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.mixin.accessor.CameraAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraMixin implements CameraAccessor, RidingCameraInterface {
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;

    @Shadow protected abstract void setPosition(Vec3 pos);

    @Unique
    private double cobblemon$lastHandledRotationTime = Double.MIN_VALUE;
    @Unique public double cobblemon$frameTime = 0.0;

    @Shadow private float partialTickTime;

    // Used to help in camera rotation rendering when transitioning
    // from rolled to unrolled.
    @Unique public float cobblemon$returnTimer = 0;
    @Unique private float cobblemon$rollAngleStart = 0;

    // Used to help in camera rotation rendering when transitioning
    // from rolled to unrolled.
    @Override
    public float getCobblemon$returnTimer() {
       return cobblemon$returnTimer;
    }

    @Override
    public void setCobblemon$returnTimer(float time) {
        cobblemon$returnTimer = time;
    }

    @Override
    public double getCobblemon$lastHandledRotationTime() {
        return cobblemon$lastHandledRotationTime;
    }

    @Override
    public void setCobblemon$lastHandledRotationTime(double time) {
        cobblemon$lastHandledRotationTime = time;
    }

    @Override
    public double getCobblemon$frameTime() {
        return cobblemon$frameTime;
    }

    @Override
    public void setCobblemon$frameTime(double time) {
        cobblemon$frameTime = time;
    }

    @Override
    public float getCobblemon$partialTickTime() {
       return partialTickTime;
    }

    @Override
    public void setCobblemon$partialTickTime(float time) {
        partialTickTime = time;
    }

    @Override
    public float getCobblemon$rollAngleStart() {
        return cobblemon$rollAngleStart;
    }

    @Override
    public void setCobblemon$rollAngleStart(float angle) {
        cobblemon$rollAngleStart = angle;
    }

    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    public void cobblemon$positionCamera(Camera instance, double x, double y, double z, Operation<Void> original, @Local(ordinal = 1, argsOnly = true) boolean thirdPersonReverse) {
        Entity entity = instance.getEntity();
        Entity vehicle = entity.getVehicle();
        MountedPokemonAnimationRenderController.INSTANCE.reset();

        if(vehicle instanceof Rideable){
            Vec3 position = MountedCameraRenderer.INSTANCE.getCameraPosition(
                    instance,
                    entity,
                    vehicle,
                    x,
                    y,
                    z,
                    thirdPersonReverse,
                    eyeHeightOld,
                    eyeHeight
            );

            if(position != null) {
                setPosition(position);
                return;
            }
        }

        original.call(instance, x, y, z);
    }

    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;move(FFF)V", ordinal = 0))
    public void stopMoveIfRolling(Camera instance, float zoom, float dy, float dx, Operation<Void> original) {
        Entity entity = instance.getEntity();
        Entity vehicle = entity.getVehicle();

        if (!(vehicle instanceof PokemonEntity) || Cobblemon.config.getThirdPersonViewBobbing()) {
            original.call(instance, zoom, dy, dx);
        }
    }
}
