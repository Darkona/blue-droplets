package com.darkona.dropletsofthirst.foundation.common.effect;

import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.darkona.dropletsofthirst.foundation.config.GameplayConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Adds thirst exhaustion every tick, scaled like the player's own activities, so creative players and players with
 * thirst disabled, whose thirst never ticks, do not pile it up.
 */
public class DehydrationEffect extends MobEffect {
    public DehydrationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player) {
            float amount = (float) (0.005F * (amplifier + 1) * GameplayConfig.DEHYDRATION_MULTIPLIER.get());
            player.getData(ModAttachment.PLAYER_THIRST).addScaledExhaustion(player, amount);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
