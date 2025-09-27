/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mechanics

import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import net.minecraft.advancements.critereon.MinMaxBounds

class RidingMechanic {
    class JetGlobalSettings {
        val infiniteStamina = false
        val jump = MinMaxBounds.Doubles.exactly(1.0)
        val handling = MinMaxBounds.Doubles.exactly(1.0)
        val handlingYaw = MinMaxBounds.Doubles.exactly(1.0)
        val speed = MinMaxBounds.Doubles.exactly(1.0)
        val acceleration = MinMaxBounds.Doubles.exactly(1.0)
        val stamina = MinMaxBounds.Doubles.exactly(1.0)
        val minSpeedFactor = 0.2F
        val deccelRate = 0.1F
        val gravity = 0

        @Transient
        val struct = QueryStruct(hashMapOf())
            .addFunction("infinite_stamina") { DoubleValue(infiniteStamina) }
            .addFunction("min_jump") { DoubleValue(jump.min.get()) }
            .addFunction("max_jump") { DoubleValue(jump.max.get()) }
            .addFunction("min_handling") { DoubleValue(handling.min.get()) }
            .addFunction("max_handling") { DoubleValue(handling.max.get()) }
            .addFunction("min_handling_yaw") { DoubleValue(handlingYaw.min.get()) }
            .addFunction("max_handling_yaw") { DoubleValue(handlingYaw.max.get()) }
            .addFunction("min_speed") { DoubleValue(speed.min.get()) }
            .addFunction("max_speed") { DoubleValue(speed.max.get()) }
            .addFunction("min_acceleration") { DoubleValue(acceleration.min.get()) }
            .addFunction("max_acceleration") { DoubleValue(acceleration.max.get()) }
            .addFunction("min_stamina") { DoubleValue(stamina.min.get()) }
            .addFunction("max_stamina") { DoubleValue(stamina.max.get()) }
            .addFunction("min_speed_factor") { DoubleValue(minSpeedFactor) }
            .addFunction("deccel_rate") { DoubleValue(deccelRate) }
            .addFunction("gravity") { DoubleValue(gravity) }
    }

    val jet = JetGlobalSettings()

    @Transient
    val struct = QueryStruct(hashMapOf())
        .addFunction("jet") { jet.struct }
}