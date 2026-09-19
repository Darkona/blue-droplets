package com.darkona.dropletsofthirst.foundation.common.effect;

import com.darkona.dropletsofthirst.api.event.ThirstChangeEvent;
import com.darkona.dropletsofthirst.content.thirst.VampireThirst;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.darkona.dropletsofthirst.foundation.config.GameplayConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Every {@code effects.quenchnessIntervalTicks}, restores (level) thirst and quenched, like Regeneration for health.
 * Not to vampires: only blood hydrates them.
 */
public class QuenchnessEffect extends MobEffect {
    public QuenchnessEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player && VampireThirst.canHydrate(ItemStack.EMPTY, player))
            player.getData(ModAttachment.PLAYER_THIRST).hydrate(player, amplifier + 1, amplifier + 1, false, ThirstChangeEvent.Cause.DRINK);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % GameplayConfig.QUENCHNESS_INTERVAL_TICKS.get() == 0;
    }
}
