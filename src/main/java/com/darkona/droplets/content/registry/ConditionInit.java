package com.darkona.droplets.content.registry;

import com.mojang.serialization.MapCodec;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.foundation.config.LootConfigCondition;
import com.darkona.droplets.foundation.config.PurityEnabledCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ConditionInit {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, BlueDroplets.ID);

    public static final Supplier<MapCodec<LootConfigCondition>> LOOT_CONFIG_CONDITION = CONDITION_CODECS.register("loot_config", () -> LootConfigCondition.CODEC);
    public static final Supplier<MapCodec<PurityEnabledCondition>> PURITY_ENABLED_CONDITION = CONDITION_CODECS.register("purity_enabled", () -> PurityEnabledCondition.CODEC);
}
