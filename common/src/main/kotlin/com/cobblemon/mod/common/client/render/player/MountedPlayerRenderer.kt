/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.player

import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationRepository
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.player.AbstractClientPlayer

/**
 * @author landonjw
 */
object MountedPlayerRenderer {
    @JvmField
    var shouldApplyRootAnimation = false

    @JvmField
    val relevantPartsByName = mutableMapOf(
        "body" to ModelPart(emptyList(), emptyMap()),
    )

    fun animate(
        pokemonEntity: PokemonEntity,
        player: AbstractClientPlayer,
        headYaw: Float,
        headPitch: Float,
        ageInTicks: Float,
        limbSwing: Float,
        limbSwingAmount: Float
    ) {
        // We need to reset the root because it's a synthetic part, Minecraft won't do this for us.
        val root = relevantPartsByName["body"] ?: return
        root.x = 0F
        root.y = 0F
        root.z = 0F
        root.xRot = 0F
        root.yRot = 0F
        root.zRot = 0F

        val seatIndex = pokemonEntity.passengers.indexOf(player).takeIf { it != -1 && it < pokemonEntity.seats.size } ?: return
        val seat = pokemonEntity.seats[seatIndex]
        val animations = seat.poseAnimations?.firstOrNull { it.poseTypes.isEmpty() || it.poseTypes.contains(pokemonEntity.getCurrentPoseType()) }?.animations
            ?: return
        val state = pokemonEntity.delegate as PokemonClientDelegate

        relevantPartsByName.values.forEach {
            it.xRot = 0F
            it.yRot = 0F
            it.zRot = 0F
        }

        state.runtime.environment.setSimpleVariable("age_in_ticks", DoubleValue(ageInTicks.toDouble()))
        state.runtime.environment.setSimpleVariable("limb_swing", DoubleValue(limbSwing.toDouble()))
        state.runtime.environment.setSimpleVariable("limb_swing_amount", DoubleValue(limbSwingAmount.toDouble()))

        // TODO add the q.player reference, requires a cached thing or something on the client idk

        for (animation in animations) {
            val bedrockAnimation = BedrockAnimationRepository.getAnimationOrNull(animation.fileName, animation.animationName) ?: continue
            bedrockAnimation.run(
                context = null,
                relevantPartsByName = relevantPartsByName,
                rootPart = null,
                state = state,
                animationSeconds = state.animationSeconds,
                limbSwing = limbSwing,
                limbSwingAmount = limbSwingAmount,
                ageInTicks = ageInTicks,
                intensity = 1F
            )
        }
    }

    fun animateRoot(stack: PoseStack) {
        val root = relevantPartsByName["body"] ?: return
        val rootOffset = Vec3(root.x.toDouble() / 24F, root.y.toDouble() / 24F, root.z.toDouble() / 24F)
        val rotation = Vec3(root.xRot.toDouble(), root.yRot.toDouble(), root.zRot.toDouble())
        stack.mulPose(Axis.ZP.rotation(rotation.z.toFloat()))
        stack.mulPose(Axis.YP.rotation(rotation.y.toFloat()))
        stack.mulPose(Axis.XP.rotation(rotation.x.toFloat()))
        stack.translate(rootOffset.x.toFloat(), rootOffset.y.toFloat(), rootOffset.z.toFloat())
    }
}
