package com.darkona.droplets.content.registry;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.foundation.common.item.TerracottaBowlItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
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
        ITEMS = DeferredRegister.createItems(BlueDroplets.ID);
        CLAY_BOWL = ITEMS.registerSimpleItem("clay_bowl");
        TERRACOTTA_BOWL = ITEMS.registerItem("terracotta_bowl", TerracottaBowlItem::new);
        // Drunk like a water bottle (the drink component) and handed back as an empty bowl, into the inventory or dropped.
        TERRACOTTA_WATER_BOWL = ITEMS.registerSimpleItem("terracotta_water_bowl", () -> new Item.Properties()
                .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                .usingConvertsTo(TERRACOTTA_BOWL.get())
        );
    }
}
