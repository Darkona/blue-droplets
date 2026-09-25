package com.darkona.dropletsofthirst.content.registry;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.api.PurityLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.List;

/**
 * Remaps the ids of Thirst Was Taken ({@code thirst:*}) to {@code droplets_of_thirst:*} in worlds it saved, so items,
 * blocks and effects load under the new ids; saving writes the new id, so each entry migrates once. The player data
 * ({@code ModAttachment}) and the purity on items and fluids ({@code ThirstComponent}) migrate on their own.
 */
public final class LegacyIds
{
    public static final String LEGACY_NAMESPACE = "thirst";

    private static final List<String> ITEMS = List.of("clay_bowl", "terracotta_bowl", "terracotta_water_bowl", "sand_filter");
    private static final List<String> SAND_FILTER = List.of("sand_filter");
    private static final List<String> EFFECTS = List.of("quenchness");

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

    /**
     * Forge 1.18.2 fires one generic {@code MissingMappings} event per registry.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register()
    {
        MinecraftForge.EVENT_BUS.addGenericListener(Item.class, (RegistryEvent.MissingMappings<Item> event) -> remap(event, ITEMS));
        MinecraftForge.EVENT_BUS.addGenericListener(Block.class, (RegistryEvent.MissingMappings<Block> event) -> remap(event, SAND_FILTER));
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntityType.class, (RegistryEvent.MissingMappings event) -> remap(event, SAND_FILTER));
        MinecraftForge.EVENT_BUS.addGenericListener(MobEffect.class, (RegistryEvent.MissingMappings<MobEffect> event) -> remap(event, EFFECTS));
    }

    private static <T extends IForgeRegistryEntry<T>> void remap(RegistryEvent.MissingMappings<T> event, List<String> paths)
    {
        for (RegistryEvent.MissingMappings.Mapping<T> mapping : event.getMappings(LEGACY_NAMESPACE))
        {
            String path = mapping.key.getPath();
            if (!paths.contains(path))
                continue;
            T target = mapping.registry.getValue(DropletsOfThirst.asResource(path));
            if (target != null)
                mapping.remap(target);
        }
    }
}
