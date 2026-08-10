package com.darkona.droplets.foundation.config;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.NotNull;

/**
 * {@code blue_droplets:quenchness_potion}: the brewing recipes of the Potion of Quenchness load only with
 * {@code effects.quenchnessPotion}. Checked when datapacks load, like every NeoForge load condition.
 */
public record QuenchnessPotionCondition() implements ICondition
{
    public static final QuenchnessPotionCondition INSTANCE = new QuenchnessPotionCondition();
    public static final MapCodec<QuenchnessPotionCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    @Override
    public boolean test(ICondition.@NotNull IContext context)
    {
        return GameplayConfig.QUENCHNESS_POTION.get();
    }

    @Override
    public @NotNull MapCodec<? extends ICondition> codec()
    {
        return CODEC;
    }
}
