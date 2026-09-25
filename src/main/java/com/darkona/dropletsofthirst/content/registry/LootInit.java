package com.darkona.dropletsofthirst.content.registry;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.common.loot.AddTableModifier;
import com.darkona.dropletsofthirst.foundation.common.loot.OptionalItemEntry;
import net.minecraft.core.Registry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Loot types the chest loot of Droplets of Thirst needs on Forge 1.18.2 (later versions have them built in).
 */
public final class LootInit
{
    private static final DeferredRegister<GlobalLootModifierSerializer<?>> MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.LOOT_MODIFIER_SERIALIZERS, DropletsOfThirst.ID);
    private static final DeferredRegister<LootPoolEntryType> ENTRIES = DeferredRegister.create(Registry.LOOT_ENTRY_REGISTRY, DropletsOfThirst.ID);

    public static final RegistryObject<AddTableModifier.Serializer> ADD_TABLE = MODIFIERS.register("add_table", AddTableModifier.Serializer::new);
    public static final RegistryObject<LootPoolEntryType> OPTIONAL_ITEM = ENTRIES.register("optional_item", () -> new LootPoolEntryType(new OptionalItemEntry.Serializer()));

    private LootInit() {}

    public static void register(IEventBus modBus)
    {
        MODIFIERS.register(modBus);
        ENTRIES.register(modBus);
    }
}
