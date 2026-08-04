package com.darkona.droplets.content.registry;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.PurityLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;

/**
 * Registry aliases from the Thirst Was Taken namespace ({@code thirst:*}) to {@code blue_droplets:*},
 * so items, blocks, block entities, effects and the player attachment saved by Thirst Was Taken load under the new
 * ids. Saving writes the new id, so each entry migrates once. The purity component is the exception: Thirst Was
 * Taken had four levels, so {@code thirst:purity} loads as {@link ThirstComponent#LEGACY_PURITY}, which stacks turn
 * into {@link ThirstComponent#PURITY} on the new scale as they load ({@link #purityFromLegacy}).
 * Never remove: unloaded chunks and offline players can keep old ids indefinitely.
 */
public final class LegacyIds
{
    public static final String LEGACY_NAMESPACE = "thirst";

    private static final Map<ResourceKey<? extends Registry<?>>, List<String>> ALIASES = Map.of(
            Registries.ITEM, List.of("clay_bowl", "terracotta_bowl", "terracotta_water_bowl"),
            Registries.MOB_EFFECT, List.of("quenchness"),
            NeoForgeRegistries.Keys.ATTACHMENT_TYPES, List.of("player_thirst"),
            NeoForgeRegistries.Keys.CONDITION_CODECS, List.of("loot_config")
    );

    /** Blue Droplets level of each Thirst Was Taken level: dirty, slightly dirty, acceptable, purified. */
    private static final int[] LEGACY_PURITY = {PurityLevel.CONTAMINATED.level(), PurityLevel.DIRTY.level(), PurityLevel.ACCEPTABLE.level(), PurityLevel.PURE.level()};

    private LegacyIds() {}

    /**
     * A Thirst Was Taken purity (0-3) as a Blue Droplets level; anything else, like -1 for "no fixed purity", is kept.
     */
    public static int purityFromLegacy(int purity)
    {
        return purity >= 0 && purity < LEGACY_PURITY.length ? LEGACY_PURITY[purity] : purity;
    }

    public static void register(IEventBus modBus)
    {
        modBus.addListener(LegacyIds::addAliases);
    }

    private static void addAliases(RegisterEvent event)
    {
        if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE))
            event.getRegistry().addAlias(Identifier.fromNamespaceAndPath(LEGACY_NAMESPACE, "purity"), BlueDroplets.asResource("legacy_purity"));
        List<String> paths = ALIASES.get(event.getRegistryKey());
        if (paths == null)
            return;

        Registry<?> registry = event.getRegistry();
        for (String path : paths)
            registry.addAlias(Identifier.fromNamespaceAndPath(LEGACY_NAMESPACE, path), BlueDroplets.asResource(path));
    }
}
