package com.darkona.droplets.foundation.tab;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ItemInit;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ThirstTab
{
    private static final DeferredRegister<CreativeModeTab> TAB_REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BlueDroplets.ID);


    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THIRST_TAB = TAB_REGISTER.register(BlueDroplets.ID,
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + BlueDroplets.ID))
                    .icon(ItemInit.TERRACOTTA_WATER_BOWL.get()::getDefaultInstance)
                    .displayItems((displayParameters, output) -> output.acceptAll(DisplayItems()))
                    .build());

    public static void register(IEventBus modEventBus) {
        TAB_REGISTER.register(modEventBus);
    }

    public static Collection<ItemStack> DisplayItems() {
        List<ItemStack> list = new ArrayList<>();

        addPurities(list, new ItemStack(Items.WATER_BUCKET));
        addPurities(list, PotionContents.createItemStack(Items.POTION, Potions.WATER));
        list.add(ItemInit.CLAY_BOWL.get().getDefaultInstance());
        list.add(ItemInit.TERRACOTTA_BOWL.get().getDefaultInstance());
        addPurities(list, new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()));

        return list;
    }

    private static void addPurities(List<ItemStack> list, ItemStack stack) {
        for (int purity = WaterPurity.MIN_PURITY; purity <= WaterPurity.MAX_PURITY; purity++)
            list.add(WaterPurity.addPurity(stack.copy(), purity));
    }
}
