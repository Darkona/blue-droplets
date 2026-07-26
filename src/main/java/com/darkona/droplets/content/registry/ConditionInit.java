package com.darkona.droplets.content.registry;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.foundation.config.LootConfigCondition;
import com.darkona.droplets.foundation.config.LootConfigLootCondition;
import com.darkona.droplets.foundation.config.PurityEnabledCondition;
import com.darkona.droplets.foundation.config.PurityEnabledLootCondition;
import net.minecraft.core.Registry;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ConditionInit {
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS = DeferredRegister.create(Registry.LOOT_ITEM_REGISTRY, BlueDroplets.ID);

    public static final RegistryObject<LootItemConditionType> PURITY_ENABLED_LOOT_CONDITION = LOOT_CONDITIONS.register("purity_enabled", () -> new LootItemConditionType(PurityEnabledLootCondition.SERIALIZER));
    public static final RegistryObject<LootItemConditionType> LOOT_CONFIG_LOOT_CONDITION = LOOT_CONDITIONS.register("loot_config", () -> new LootItemConditionType(LootConfigLootCondition.SERIALIZER));

    /**
     * Recipe conditions go to Forge's own table (not a registry in 1.18.2); loot conditions to the vanilla registry.
     */
    public static void register(IEventBus modBus) {
        CraftingHelper.register(LootConfigCondition.SERIALIZER);
        CraftingHelper.register(PurityEnabledCondition.SERIALIZER);
        LOOT_CONDITIONS.register(modBus);
    }
}
