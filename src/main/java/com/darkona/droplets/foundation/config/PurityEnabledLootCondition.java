package com.darkona.droplets.foundation.config;

import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ConditionInit;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

/**
 * Loot condition {@code bluedroplets:purity_enabled}: true while {@code purity.enabled} is on. Checked each time loot
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
    public @NotNull LootItemConditionType getType()
    {
        return ConditionInit.PURITY_ENABLED_LOOT_CONDITION.get();
    }
}
