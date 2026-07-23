package com.darkona.droplets.content.data;

import com.darkona.droplets.BlueDroplets;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;

import java.util.List;

/**
 * The data maps of later versions, read by reload listeners from the same files ({@link DataMapType}). The server
 * sends clients what they need already resolved: the drink table ({@code ThirstValuesSyncMessage}).
 */
public final class DropletsDataMaps
{
    /** {@code data/<ns>/data_maps/item/drinks.json}. */
    public static final DataMapType<Item, DrinkValues> DRINKS = DataMapType
            .create(BlueDroplets.asResource("drinks"), Registry.ITEM_REGISTRY, DrinkValues.CODEC);
    /** {@code data/<ns>/data_maps/block/hydrating_blocks.json}: block foods (cake); server only, {@code purity} unused. */
    public static final DataMapType<Block, DrinkValues> HYDRATING_BLOCKS = DataMapType
            .create(BlueDroplets.asResource("hydrating_blocks"), Registry.BLOCK_REGISTRY, DrinkValues.CODEC);
    /** {@code data/<ns>/data_maps/worldgen/biome/biome_water.json}; server only. */
    public static final DataMapType<Biome, BiomeWater> BIOME_WATER = DataMapType
            .create(BlueDroplets.asResource("biome_water"), Registry.BIOME_REGISTRY, BiomeWater.CODEC);
    /** {@code data/<ns>/data_maps/dimension_type/dimension_water.json}; server only. */
    public static final DataMapType<DimensionType, DimensionWater> DIMENSION_WATER = DataMapType
            .create(BlueDroplets.asResource("dimension_water"), Registry.DIMENSION_TYPE_REGISTRY, DimensionWater.CODEC);

    private static final List<DataMapType<?, ?>> ALL = List.of(DRINKS, HYDRATING_BLOCKS, BIOME_WATER, DIMENSION_WATER);

    private DropletsDataMaps() {}

    public static void addReloadListeners(AddReloadListenerEvent event)
    {
        for (DataMapType<?, ?> type : ALL)
            event.addListener(type.reloadListener(event.getConditionContext()));
    }

    /**
     * Resolves ids and tags once the server's tags are bound; before the tables that read them are rebuilt
     * ({@code ThirstHelper} rebuilds at {@code LOWEST}).
     */
    public static void bind(TagsUpdatedEvent event)
    {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD)
            return;
        for (DataMapType<?, ?> type : ALL)
            type.bind(event.getRegistryAccess());
    }
}
