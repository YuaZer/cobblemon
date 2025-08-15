/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;


import com.cobblemon.mod.common.CobblemonMemories;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.ai.tasks.PathToBeeHiveTask;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeehiveBlockEntity.Occupant.class)
public abstract class BeeOccupantMixin {


    @Inject(method = "createEntity", at = @At("RETURN"), cancellable = true)
    private void cobblemon$createEntity(Level level, BlockPos pos, CallbackInfoReturnable<Entity> cir) {
        // TODO: Find a solution that isn't a mixin, pokemon aren't beehive_inhabitants
        // Needing to replicate all the post op stuff on the hive isn't great
        // Even then may still need to set the position data
        if (cir.getReturnValue() == null) {
            final BeehiveBlockEntity.Occupant occupant = (BeehiveBlockEntity.Occupant) (Object) this;
            CompoundTag compoundTag = occupant.entityData().copyTag();
            Entity entity = EntityType.loadEntityRecursive(compoundTag, level, (entityx) -> entityx);
            if (entity instanceof PokemonEntity pokemonEntity) {
                var state = level.getBlockState(pos);
                var facing = state.getValue(HorizontalDirectionalBlock.FACING);
                var newPos = pos.relative(facing);
                entity.setPos(newPos.getCenter());
                entity.yRotO = facing.toYRot();
                // Do honey logic
                var brain = pokemonEntity.getBrain();
                // When we became part of the beehive's data we lost all Brain memories.
                // Restoring the pollinated flag from the compound tag
                var hasNectar = compoundTag.getBoolean("HasNectar");
                brain.setMemory(CobblemonMemories.INSTANCE.getHIVE_LOCATION(), pos);
                if (hasNectar) {
                    // Remove nectar and reset got to hive cooldown
                    brain.eraseMemory(CobblemonMemories.INSTANCE.getPOLLINATED());
                    brain.setMemoryWithExpiry(CobblemonMemories.INSTANCE.getHIVE_COOLDOWN(), true, PathToBeeHiveTask.INSTANCE.getSTAY_OUT_OF_HIVE_COOLDOWN());
                    // Increment honey level of the hive
                    if (state.is(BlockTags.BEEHIVES, (blockStateBase) -> blockStateBase.hasProperty(BeehiveBlock.HONEY_LEVEL))) {
                        int i = state.getValue(BeehiveBlock.HONEY_LEVEL);
                        if (i < 5) {
                            int j = level.random.nextInt(100) == 0 ? 2 : 1;
                            if (i + j > 5) {
                                --j;
                            }
                            level.setBlockAndUpdate(pos, (BlockState) state.setValue(BeehiveBlock.HONEY_LEVEL, i + j));
                        }

                    }
                }
                cir.setReturnValue(entity);
            }
        }
    }

    @Inject(method = "of", at = @At("HEAD"), cancellable = true)
    private static void cobblemon$of(Entity entity, CallbackInfoReturnable<BeehiveBlockEntity.Occupant> cir) {
        if (entity instanceof PokemonEntity pokemonEntity) {
            CompoundTag compoundTag = new CompoundTag();
            entity.save(compoundTag);
//            List var10000 = BeehiveBlockEntity.IGNORED_BEE_TAGS;
//            Objects.requireNonNull(compoundTag);
//            var10000.forEach(compoundTag::remove);


            Boolean hasNectar = pokemonEntity.getBrain().getMemory(CobblemonMemories.INSTANCE.getPOLLINATED()).orElse(false);
            // All brain memories are lost when we become a part of beehive
            // Need to store anything we want to read later as a tag
            // for when the entity gets rehydrated
            compoundTag.putBoolean("HasNectar", hasNectar);
            cir.setReturnValue(new BeehiveBlockEntity.Occupant(CustomData.of(compoundTag), 0, hasNectar ? 2400 : 600));
            cir.cancel();
        }

    }
}

