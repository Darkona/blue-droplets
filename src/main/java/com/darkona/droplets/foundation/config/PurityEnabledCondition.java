package com.darkona.droplets.foundation.config;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.NotNull;

/**
 * {@code bluedroplets:purity_enabled}: true when {@code purity.enabled} is on; checked when datapacks load.
 */
public record PurityEnabledCondition() implements ICondition
{
    public static final PurityEnabledCondition INSTANCE = new PurityEnabledCondition();
    public static final MapCodec<PurityEnabledCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    @Override
    public boolean test(ICondition.@NotNull IContext context)
    {
        return PurityConfig.ENABLED.get();
    }

    @Override
    public @NotNull MapCodec<? extends ICondition> codec()
    {
        return CODEC;
    }
}
