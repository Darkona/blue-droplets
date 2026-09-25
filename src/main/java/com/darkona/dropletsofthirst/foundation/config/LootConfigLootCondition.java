package com.darkona.dropletsofthirst.foundation.config;

import com.darkona.dropletsofthirst.content.registry.ConditionInit;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

/**
 * Loot condition {@code droplets_of_thirst:loot_config}: true with {@code loot.enabled}. The global loot modifiers that add
 * Droplets of Thirst's chest loot carry it; checked on every roll, so a change applies at once.
 */
public final class LootConfigLootCondition implements LootItemCondition
{
    public static final LootConfigLootCondition INSTANCE = new LootConfigLootCondition();
    public static final Serializer<LootConfigLootCondition> SERIALIZER = new Serializer<>()
    {
        @Override
        public void serialize(JsonObject json, LootConfigLootCondition value, JsonSerializationContext context) {}

        @Override
        public LootConfigLootCondition deserialize(JsonObject json, JsonDeserializationContext context)
        {
            return INSTANCE;
        }
    };

    private LootConfigLootCondition() {}

    @Override
    public boolean test(LootContext context)
    {
        return GameplayConfig.LOOT.get();
    }

    @Override
    public @NotNull LootItemConditionType getType()
    {
        return ConditionInit.LOOT_CONFIG_LOOT_CONDITION.get();
    }
}
