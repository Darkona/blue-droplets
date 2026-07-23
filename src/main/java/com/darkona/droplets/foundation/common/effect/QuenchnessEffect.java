package com.darkona.droplets.foundation.common.effect;

import com.darkona.droplets.api.event.ThirstChangeEvent;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.GameplayConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Every {@code effects.quenchnessIntervalTicks}, restores (level) thirst and quenched, like Regeneration for health.
 */
public class QuenchnessEffect extends MobEffect {
    public QuenchnessEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level.isClientSide && entity instanceof Player player)
            ModAttachment.thirst(player).hydrate(player, amplifier + 1, amplifier + 1, false, ThirstChangeEvent.Cause.DRINK);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % GameplayConfig.QUENCHNESS_INTERVAL_TICKS.get() == 0;
    }
}
