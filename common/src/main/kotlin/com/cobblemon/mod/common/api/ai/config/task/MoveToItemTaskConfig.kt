/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.ai.config.task

import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.CobblemonSensors
import com.cobblemon.mod.common.api.ai.BehaviourConfigurationContext
import com.cobblemon.mod.common.api.ai.ExpressionOrEntityVariable
import com.cobblemon.mod.common.api.ai.WrapperLivingEntityTask
import com.cobblemon.mod.common.api.ai.asVariables
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.entity.pokemon.ai.tasks.MoveToItemTask
import com.cobblemon.mod.common.pokemon.ai.ObtainableItem
import com.cobblemon.mod.common.util.asExpression
import com.cobblemon.mod.common.util.resolveObject
import com.mojang.datafixers.util.Either
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.behavior.BehaviorControl
import net.minecraft.world.entity.ai.memory.MemoryModuleType

class MoveToItemTaskConfig : SingleTaskConfig {
    val condition: ExpressionOrEntityVariable = Either.left("true".asExpression())
    var speedMultiplier: ExpressionOrEntityVariable = Either.left("0.6".asExpression())
    var maxDistance: ExpressionOrEntityVariable = Either.left("7.0".asExpression())

    override fun getVariables(entity: LivingEntity) = listOf(
        condition,
        speedMultiplier,
        maxDistance
    ).asVariables()

    override fun createTask(
        entity: LivingEntity,
        behaviourConfigurationContext: BehaviourConfigurationContext
    ): BehaviorControl<in LivingEntity>? {
        behaviourConfigurationContext.addMemories(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.WALK_TARGET, MemoryModuleType.LOOK_TARGET)
        behaviourConfigurationContext.addSensors(CobblemonSensors.POKEMON_NEARBY_WANTED_ITEM)
//        val runtime = MoLangRuntime().setup()
//        val item = runtime.resolveObject(itemEntry.asExpression()).obj as ObtainableItem


        return WrapperLivingEntityTask(
            MoveToItemTask.create(
                condition = condition.asExpression(),
                speedMultiplier = speedMultiplier.asExpression(),
                maxDistance = maxDistance.asExpression()
            ),
            PokemonEntity::class.java
        )
    }
}