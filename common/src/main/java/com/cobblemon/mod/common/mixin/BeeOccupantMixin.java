/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;


import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeehiveBlockEntity.Occupant.class)
public abstract class BeeOccupantMixin {


    @Inject(method = "createEntity", at = @At("RETURN"),
            cancellable = true)
    private void cobblemon$createEntity(Level level, BlockPos pos, CallbackInfoReturnable<Entity> cir) {
        // TODO: Find a solution that isn't a mixin, pokemon aren't beehive_inhabitants
        // Even then may still need to set the position data
        if (cir.getReturnValue() == null) {
            final BeehiveBlockEntity.Occupant occupant = (BeehiveBlockEntity.Occupant) (Object) this;
            CompoundTag compoundTag = occupant.entityData().copyTag();
            Entity entity = EntityType.loadEntityRecursive(compoundTag, level, (entityx) -> entityx);
            if (entity != null) {
                var state = level.getBlockState(pos);
                var facing = state.getValue(HorizontalDirectionalBlock.FACING);
                var newPos = pos.relative(facing);
                entity.setPos(newPos.getCenter());
                cir.setReturnValue(entity);
            }
        }
    }
}

