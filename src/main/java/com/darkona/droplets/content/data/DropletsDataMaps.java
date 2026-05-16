package com.darkona.droplets.content.data;

import com.darkona.droplets.BlueDroplets;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

public final class DropletsDataMaps
{
    /** {@code data/<ns>/data_maps/item/drinks.json}; synced, not mandatory. */
    public static final DataMapType<Item, DrinkValues> DRINKS = DataMapType
            .builder(BlueDroplets.asResource("drinks"), Registries.ITEM, DrinkValues.CODEC)
            .synced(DrinkValues.CODEC, false)
            .build();

    private DropletsDataMaps() {}

    public static void register(RegisterDataMapTypesEvent event)
    {
        event.register(DRINKS);
    }
}
