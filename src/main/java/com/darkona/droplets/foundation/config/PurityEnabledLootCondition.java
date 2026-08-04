package com.darkona.droplets.foundation.config;

import com.darkona.droplets.content.purity.WaterPurity;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

/**
 * Loot condition {@code blue_droplets:purity_enabled}: true while {@code purity.enabled} is on. Checked each time loot
 * is rolled; the chest loot tables put it on the {@code set_components} function that stores a purity.
 */
public record PurityEnabledLootCondition() implements LootItemCondition
{
    public static final PurityEnabledLootCondition INSTANCE = new PurityEnabledLootCondition();
    public static final MapCodec<PurityEnabledLootCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(LootContext context)
    {
        return WaterPurity.enabled();
    }

    @Override
    public @NotNull MapCodec<PurityEnabledLootCondition> codec()
    {
        return CODEC;
    }
}
