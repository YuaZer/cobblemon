/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.accessor;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("level")
    BlockGetter getLevel();

    @Accessor("entity")
    Entity getEntity();

    @Accessor("rotation")
    Quaternionf getRotation();

    @Accessor("forwards")
    Vector3f getForwards();

    @Accessor("up")
    Vector3f getUp();

    @Accessor("left")
    Vector3f getLeft();

    @Accessor("xRot")
    float getXRot();
    @Accessor("xRot")
    void setXRot(float xRot);

    @Accessor("yRot")
    float getYRot();
    @Accessor("yRot")
    void setYRot(float yRot);


}