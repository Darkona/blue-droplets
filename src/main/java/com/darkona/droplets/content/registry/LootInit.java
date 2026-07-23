package com.darkona.droplets.content.registry;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.foundation.common.loot.AddTableModifier;
import com.darkona.droplets.foundation.common.loot.OptionalItemEntry;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Loot types the chest loot of Blue Droplets needs on Forge 1.19.2 (later versions have them built in).
 */
public final class LootInit
{
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, BlueDroplets.ID);
    private static final DeferredRegister<LootPoolEntryType> ENTRIES = DeferredRegister.create(Registry.LOOT_ENTRY_REGISTRY, BlueDroplets.ID);

    public static final RegistryObject<Codec<AddTableModifier>> ADD_TABLE = MODIFIERS.register("add_table", () -> AddTableModifier.CODEC);
    public static final RegistryObject<LootPoolEntryType> OPTIONAL_ITEM = ENTRIES.register("optional_item", () -> new LootPoolEntryType(new OptionalItemEntry.Serializer()));

    private LootInit() {}

    public static void register(IEventBus modBus)
    {
        MODIFIERS.register(modBus);
        ENTRIES.register(modBus);
    }
}
