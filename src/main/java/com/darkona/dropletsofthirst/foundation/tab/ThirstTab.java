package com.darkona.dropletsofthirst.foundation.tab;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.registry.ItemInit;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ThirstTab
{
    private static final DeferredRegister<CreativeModeTab> TAB_REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DropletsOfThirst.ID);


    public static final RegistryObject<CreativeModeTab> THIRST_TAB = TAB_REGISTER.register(DropletsOfThirst.ID,
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + DropletsOfThirst.ID))
                    .icon(ItemInit.TERRACOTTA_WATER_BOWL.get()::getDefaultInstance)
                    .displayItems((displayParameters, output) -> output.acceptAll(DisplayItems()))
                    .build());

    public static void register(IEventBus modEventBus) {
        TAB_REGISTER.register(modEventBus);
    }

    public static Collection<ItemStack> DisplayItems() {
        List<ItemStack> list = new ArrayList<>();

        addPurities(list, new ItemStack(Items.WATER_BUCKET));
        addPurities(list, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
        list.add(ItemInit.CLAY_BOWL.get().getDefaultInstance());
        list.add(ItemInit.TERRACOTTA_BOWL.get().getDefaultInstance());
        addPurities(list, new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()));

        return list;
    }

    /**
     * One stack per purity, or the plain stack once when purity is off.
     */
    private static void addPurities(List<ItemStack> list, ItemStack stack) {
        if (!WaterPurity.enabled()) {
            list.add(stack);
            return;
        }
        for (int purity = WaterPurity.MIN_PURITY; purity <= WaterPurity.MAX_PURITY; purity++)
            list.add(WaterPurity.addPurity(stack.copy(), purity));
    }
}
