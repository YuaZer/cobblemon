package com.cobblemon.mod.common.advancement.criterion

import com.cobblemon.mod.common.api.riding.RidingStyle
import com.cobblemon.mod.common.api.riding.stats.RidingStat
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.server.level.ServerPlayer
import java.util.Optional

class RidingStatBoostContext(val pokemon: PokemonEntity)

class RidingStatBoostCriterion(
    playerCtx: Optional<ContextAwarePredicate>,
    val rideStat: String,
    val statValue : Double
) : SimpleCriterionCondition<RidingStatBoostContext>(playerCtx) {

    companion object {
        val CODEC: Codec<RidingStatBoostCriterion> = RecordCodecBuilder.create { it.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter { it.playerCtx },
            Codec.STRING.optionalFieldOf("ride_stat", "any").forGetter { it.rideStat },
            Codec.DOUBLE.optionalFieldOf("stat_value", 0.0).forGetter { it.statValue }
        ).apply(it, ::RidingStatBoostCriterion) }
    }

    override fun matches(player: ServerPlayer, context: RidingStatBoostContext) : Boolean {
        RidingStyle.entries.forEach { ridingStyle ->
            RidingStat.entries.forEach { rideStat ->
                if (rideStat.name.equals(this.rideStat, true) || this.rideStat == "any") {
                    val value = context.pokemon.getRideStat(rideStat, ridingStyle, 0.0, 100.0)
                    if (value >= this.statValue) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
