package com.darkona.droplets.foundation.config;

import com.darkona.droplets.BlueDroplets;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * {@code blue_droplets:loot_config}: true with {@code loot.enabled}. Forge 1.20.1 only reads load conditions in
 * recipes and advancements, so the chest loot uses the loot condition of the same id instead
 * ({@link LootConfigLootCondition}), checked on every roll.
 */
public final class LootConfigCondition implements ICondition
{
    public static final ResourceLocation ID = BlueDroplets.asResource("loot_config");
    public static final LootConfigCondition INSTANCE = new LootConfigCondition();
    public static final IConditionSerializer<LootConfigCondition> SERIALIZER = new IConditionSerializer<>()
    {
        @Override
        public void write(JsonObject json, LootConfigCondition value) {}

        @Override
        public LootConfigCondition read(JsonObject json)
        {
            return INSTANCE;
        }

        @Override
        public ResourceLocation getID()
        {
            return ID;
        }
    };

    private LootConfigCondition() {}

    @Override
    public ResourceLocation getID()
    {
        return ID;
    }

    @Override
    public boolean test(IContext context)
    {
        return GameplayConfig.LOOT.get();
    }
}
