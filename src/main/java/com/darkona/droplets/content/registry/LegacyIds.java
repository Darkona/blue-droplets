package com.darkona.droplets.content.registry;

import com.darkona.droplets.BlueDroplets;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;

/**
 * Registry aliases from the Thirst Was Taken namespace ({@code thirst:*}) to {@code blue_droplets:*},
 * so items, blocks, block entities, effects, the purity component and the player attachment saved
 * by Thirst Was Taken load under the new ids. Saving writes the new id, so each entry migrates once.
 * Never remove: unloaded chunks and offline players can keep old ids indefinitely.
 */
public final class LegacyIds
{
    public static final String LEGACY_NAMESPACE = "thirst";

    private static final Map<ResourceKey<? extends Registry<?>>, List<String>> ALIASES = Map.of(
            Registries.ITEM, List.of("clay_bowl", "terracotta_bowl", "terracotta_water_bowl", "sand_filter"),
            Registries.BLOCK, List.of("sand_filter"),
            Registries.BLOCK_ENTITY_TYPE, List.of("sand_filter"),
            Registries.MOB_EFFECT, List.of("quenchness"),
            Registries.DATA_COMPONENT_TYPE, List.of("purity"),
            NeoForgeRegistries.Keys.ATTACHMENT_TYPES, List.of("player_thirst"),
            NeoForgeRegistries.Keys.CONDITION_CODECS, List.of("loot_config")
    );

    private LegacyIds() {}

    public static void register(IEventBus modBus)
    {
        modBus.addListener(LegacyIds::addAliases);
    }

    private static void addAliases(RegisterEvent event)
    {
        List<String> paths = ALIASES.get(event.getRegistryKey());
        if (paths == null)
            return;

        Registry<?> registry = event.getRegistry();
        for (String path : paths)
            registry.addAlias(ResourceLocation.fromNamespaceAndPath(LEGACY_NAMESPACE, path), BlueDroplets.asResource(path));
    }
}
