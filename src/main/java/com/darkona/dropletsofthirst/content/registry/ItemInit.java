package com.darkona.dropletsofthirst.content.registry;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.common.item.DrinkableItem;
import com.darkona.dropletsofthirst.foundation.common.item.TerracottaBowlItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ItemInit {
    public static final DeferredRegister.Items ITEMS;
    public static final DeferredItem<Item> CLAY_BOWL;
    public static final DeferredItem<Item>  TERRACOTTA_BOWL;
    public static final DeferredItem<Item>  TERRACOTTA_WATER_BOWL;

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    static {
        ITEMS = DeferredRegister.createItems(DropletsOfThirst.ID);
        CLAY_BOWL = ITEMS.register("clay_bowl", () -> new Item((new Item.Properties())
                .stacksTo(64)
        ));
        TERRACOTTA_BOWL = ITEMS.register("terracotta_bowl", () -> new TerracottaBowlItem((new Item.Properties())
                .stacksTo(64)
        ));
        TERRACOTTA_WATER_BOWL = ITEMS.register("terracotta_water_bowl", () -> (new DrinkableItem())
                .setContainer(TERRACOTTA_BOWL.get())
        );
    }
}