package com.darkona.dropletsofthirst.foundation.config;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * {@code droplets_of_thirst:purity_enabled}: true when {@code purity.enabled} is on; checked when datapacks load.
 */
public final class PurityEnabledCondition implements ICondition
{
    public static final ResourceLocation ID = DropletsOfThirst.asResource("purity_enabled");
    public static final PurityEnabledCondition INSTANCE = new PurityEnabledCondition();
    public static final IConditionSerializer<PurityEnabledCondition> SERIALIZER = new IConditionSerializer<>()
    {
        @Override
        public void write(JsonObject json, PurityEnabledCondition value) {}

        @Override
        public PurityEnabledCondition read(JsonObject json)
        {
            return INSTANCE;
        }

        @Override
        public ResourceLocation getID()
        {
            return ID;
        }
    };

    private PurityEnabledCondition() {}

    @Override
    public ResourceLocation getID()
    {
        return ID;
    }

    @Override
    public boolean test(IContext context)
    {
        return PurityConfig.ENABLED.get();
    }
}
