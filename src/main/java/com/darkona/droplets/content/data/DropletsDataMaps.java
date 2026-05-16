package com.darkona.droplets.content.data;

import com.darkona.droplets.BlueDroplets;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class DropletsDataMaps
{
    /** {@code data/<ns>/data_maps/item/drinks.json}; synced, not mandatory. */
    public static final DataMapType<Item, DrinkValues> DRINKS = DataMapType
            .builder(BlueDroplets.asResource("drinks"), Registries.ITEM, DrinkValues.CODEC)
            .synced(DrinkValues.CODEC, false)
            .build();
    /** {@code data/<ns>/data_maps/worldgen/biome/biome_water.json}; server only. */
    public static final DataMapType<Biome, BiomeWater> BIOME_WATER = DataMapType
            .builder(BlueDroplets.asResource("biome_water"), Registries.BIOME, BiomeWater.CODEC)
            .build();
    /** {@code data/<ns>/data_maps/dimension_type/dimension_water.json}; server only. */
    public static final DataMapType<DimensionType, DimensionWater> DIMENSION_WATER = DataMapType
            .builder(BlueDroplets.asResource("dimension_water"), Registries.DIMENSION_TYPE, DimensionWater.CODEC)
            .build();

    private DropletsDataMaps() {}

    public static void register(RegisterDataMapTypesEvent event)
    {
        event.register(DRINKS);
        event.register(BIOME_WATER);
        event.register(DIMENSION_WATER);
    }
}
