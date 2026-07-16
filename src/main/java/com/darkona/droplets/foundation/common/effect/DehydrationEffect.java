package com.darkona.droplets.foundation.common.effect;

import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.GameplayConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class DehydrationEffect extends MobEffect {
    public DehydrationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player) {
            float amount = (float) (0.005F * (amplifier + 1) * GameplayConfig.DEHYDRATION_MULTIPLIER.get());
            ModAttachment.thirst(player).addExhaustion(player, amount);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
