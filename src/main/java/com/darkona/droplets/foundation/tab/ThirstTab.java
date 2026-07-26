package com.darkona.droplets.foundation.tab;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.create.CreateCompat;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ItemInit;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The mod's creative tab. Minecraft 1.18.2 has no tab registry: the tab is a {@link CreativeModeTab} subclass, built
 * when this class loads, and lists {@link #DisplayItems()} each time it fills.
 */
public class ThirstTab extends CreativeModeTab
{
    public static final ThirstTab THIRST_TAB = new ThirstTab();

    private ThirstTab() {
        super(BlueDroplets.ID);
    }

    @Override
    public ItemStack makeIcon() {
        return ItemInit.TERRACOTTA_WATER_BOWL.get().getDefaultInstance();
    }

    @Override
    public void fillItemList(NonNullList<ItemStack> items) {
        items.addAll(DisplayItems());
    }

    public static Collection<ItemStack> DisplayItems() {
        List<ItemStack> list = new ArrayList<>();

        addPurities(list, new ItemStack(Items.WATER_BUCKET));
        addPurities(list, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
        list.add(ItemInit.CLAY_BOWL.get().getDefaultInstance());
        list.add(ItemInit.TERRACOTTA_BOWL.get().getDefaultInstance());
        addPurities(list, new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()));
        if (CreateCompat.LOADED)
            list.add(new ItemStack(CreateCompat.sandFilter()));

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
