package com.darkona.droplets.foundation.config;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.NotNull;

/**
 * {@code bluedroplets:loot_config}: data (the chest loot tables and the global loot modifier list) loads only with
 * {@code loot.enabled}. Like every NeoForge load condition it is checked when datapacks load, not per chest.
 */
public record LootConfigCondition() implements ICondition
{
    public static final LootConfigCondition INSTANCE = new LootConfigCondition();
    public static final MapCodec<LootConfigCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    @Override
    public boolean test(ICondition.@NotNull IContext context) {
        return GameplayConfig.LOOT.get();
    }

    @Override
    public @NotNull MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
