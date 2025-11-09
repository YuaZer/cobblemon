package com.cobblemon.mod.common.client

import org.spongepowered.asm.mixin.Unique

interface RidingCameraInterface {
    // Used to help in camera rotation rendering when transitioning
    // from rolled to unrolled.
    @Unique
    fun `getCobblemon$returnTimer`(): Float

    @Unique
    fun `setCobblemon$returnTimer`(time: Float)

    @Unique
    fun `getCobblemon$lastHandledRotationTime`(): Double

    @Unique
    fun `setCobblemon$lastHandledRotationTime`(time: Double)

    @Unique
    fun `getCobblemon$frameTime`(): Double

    @Unique
    fun `setCobblemon$frameTime`(time: Double)

    @Unique
    fun `getCobblemon$partialTickTime`(): Float

    @Unique
    fun `setCobblemon$partialTickTime`(time: Float)

    @Unique
    fun `getCobblemon$rollAngleStart`(): Float

    @Unique
    fun `setCobblemon$rollAngleStart`(angle: Float)
}