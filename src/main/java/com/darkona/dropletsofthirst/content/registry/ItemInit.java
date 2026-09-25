package com.darkona.dropletsofthirst.content.registry;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.common.item.DrinkableItem;
import com.darkona.dropletsofthirst.foundation.common.item.TerracottaBowlItem;
import com.darkona.dropletsofthirst.foundation.tab.ThirstTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ItemInit {
    public static final DeferredRegister<Item> ITEMS;
    public static final RegistryObject<Item> CLAY_BOWL;
    public static final RegistryObject<Item>  TERRACOTTA_BOWL;
    public static final RegistryObject<Item>  TERRACOTTA_WATER_BOWL;

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    static {
        ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DropletsOfThirst.ID);
        CLAY_BOWL = ITEMS.register("clay_bowl", () -> new Item((new Item.Properties())
                .stacksTo(64).tab(ThirstTab.THIRST_TAB)
        ));
        TERRACOTTA_BOWL = ITEMS.register("terracotta_bowl", () -> new TerracottaBowlItem((new Item.Properties())
                .stacksTo(64).tab(ThirstTab.THIRST_TAB)
        ));
        TERRACOTTA_WATER_BOWL = ITEMS.register("terracotta_water_bowl", () -> (new DrinkableItem())
                .setContainer(TERRACOTTA_BOWL.get())
        );
    }
}
