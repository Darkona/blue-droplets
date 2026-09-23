package com.darkona.dropletsofthirst.content.registry;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.api.PurityLevel;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

import java.util.List;
import java.util.Map;

/**
 * Remaps the ids of Thirst Was Taken ({@code thirst:*}) to {@code droplets_of_thirst:*} in worlds it saved, so items,
 * blocks and effects load under the new ids; saving writes the new id, so each entry migrates once. The player data
 * ({@code ModAttachment}) and the purity on items and fluids ({@code ThirstComponent}) migrate on their own.
 */
public final class LegacyIds
{
    public static final String LEGACY_NAMESPACE = "thirst";

    private static final Map<ResourceKey<? extends Registry<?>>, List<String>> REMAPS = Map.of(
            Registry.ITEM_REGISTRY, List.of("clay_bowl", "terracotta_bowl", "terracotta_water_bowl", "sand_filter"),
            Registry.BLOCK_REGISTRY, List.of("sand_filter"),
            Registry.BLOCK_ENTITY_TYPE_REGISTRY, List.of("sand_filter"),
            Registry.MOB_EFFECT_REGISTRY, List.of("quenchness")
    );

    /** Droplets of Thirst level of each Thirst Was Taken level: dirty, slightly dirty, acceptable, purified. */
    private static final int[] LEGACY_PURITY = {PurityLevel.CONTAMINATED.level(), PurityLevel.DIRTY.level(), PurityLevel.ACCEPTABLE.level(), PurityLevel.PURE.level()};

    private LegacyIds() {}

    /**
     * A Thirst Was Taken purity (0-3) as a Droplets of Thirst level; anything else, like -1 for "no fixed purity", is kept.
     */
    public static int purityFromLegacy(int purity)
    {
        return purity >= 0 && purity < LEGACY_PURITY.length ? LEGACY_PURITY[purity] : purity;
    }

    public static void register()
    {
        MinecraftForge.EVENT_BUS.addListener(LegacyIds::onMissingMappings);
    }

    private static void onMissingMappings(MissingMappingsEvent event)
    {
        List<String> paths = REMAPS.get(event.getKey());
        if (paths != null)
            remap(event, event.getKey(), paths);
    }

    @SuppressWarnings("unchecked")
    private static <T> void remap(MissingMappingsEvent event, ResourceKey<? extends Registry<?>> key, List<String> paths)
    {
        IForgeRegistry<T> registry = (IForgeRegistry<T>) event.getRegistry();
        for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings((ResourceKey<? extends Registry<T>>) key, LEGACY_NAMESPACE))
        {
            String path = mapping.getKey().getPath();
            if (!paths.contains(path))
                continue;
            T target = registry.getValue(DropletsOfThirst.asResource(path));
            if (target != null)
                mapping.remap(target);
        }
    }
}
