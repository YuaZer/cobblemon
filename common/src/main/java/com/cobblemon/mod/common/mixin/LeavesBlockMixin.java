/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.bedrockk.molang.runtime.value.DoubleValue;
import com.cobblemon.mod.common.entity.MoLangScriptingEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(BlockBehaviour.class)
public abstract class LeavesBlockMixin {
    @Inject(method = "getCollisionShape",
            at = @At(value = "HEAD"),
            cancellable = true,
            require = 0,
            remap = true)
    private void cobblemon$getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        // Early exit if not a leaves block or not an entity collision
        if (!(state.getBlock() instanceof LeavesBlock)) return;
        if (!(context instanceof EntityCollisionContext ecc)) return;

        // Early exit if entity is null or not a Pokemon
        Entity entity = ecc.getEntity();
        if (entity == null) return;
        if (!(entity instanceof MoLangScriptingEntity)) return;
        if (((MoLangScriptingEntity) entity).getConfig().getMap().getOrDefault("can_path_through_leaves", DoubleValue.ZERO).asDouble() != 1.0) return;
        cir.setReturnValue(Shapes.empty());
    }
}
