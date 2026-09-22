package com.darkona.dropletsofthirst.content.registry;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.config.LootConfigCondition;
import com.darkona.dropletsofthirst.foundation.config.LootConfigLootCondition;
import com.darkona.dropletsofthirst.foundation.config.PurityEnabledCondition;
import com.darkona.dropletsofthirst.foundation.config.PurityEnabledLootCondition;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ConditionInit {
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS = DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, DropletsOfThirst.ID);

    public static final RegistryObject<LootItemConditionType> PURITY_ENABLED_LOOT_CONDITION = LOOT_CONDITIONS.register("purity_enabled", () -> new LootItemConditionType(PurityEnabledLootCondition.SERIALIZER));
    public static final RegistryObject<LootItemConditionType> LOOT_CONFIG_LOOT_CONDITION = LOOT_CONDITIONS.register("loot_config", () -> new LootItemConditionType(LootConfigLootCondition.SERIALIZER));

    /**
     * Recipe conditions go to Forge's own table (not a registry in 1.20.1); loot conditions to the vanilla registry.
     */
    public static void register(IEventBus modBus) {
        CraftingHelper.register(LootConfigCondition.SERIALIZER);
        CraftingHelper.register(PurityEnabledCondition.SERIALIZER);
        LOOT_CONDITIONS.register(modBus);
    }
}
