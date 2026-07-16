package com.darkona.droplets.compat.jei;

import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.content.thirst.ThirstHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * An item that changes thirst, with its resolved values, for the recipe viewer's hydration category. Plain data, no
 * recipe viewer classes, so the server can build it too.
 */
public record HydrationEntry(ItemStack stack, ThirstValues values)
{
    private static final Comparator<HydrationEntry> BY_ID = Comparator.comparing(entry -> BuiltInRegistries.ITEM.getKey(entry.stack.getItem()));

    /**
     * Drinks, then foods, each sorted by item id, from the tables in use (the server's on a remote client). Items whose
     * values give nothing, and items whose values only a provider knows for a real stack, are left out.
     */
    public static List<HydrationEntry> all()
    {
        List<HydrationEntry> entries = new ArrayList<>();
        add(entries, ThirstHelper.drinkTable().keySet());
        int drinks = entries.size();
        add(entries, ThirstHelper.foodTable().keySet());
        entries.subList(0, drinks).sort(BY_ID);
        entries.subList(drinks, entries.size()).sort(BY_ID);
        return entries;
    }

    private static void add(List<HydrationEntry> entries, Collection<Item> items)
    {
        for (Item item : items)
        {
            ItemStack stack = item == Items.POTION ? PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER) : new ItemStack(item);
            ThirstValues values = ThirstHelper.valuesOf(stack);
            if (values != null && (values.thirst() != 0 || values.quenched() != 0))
                entries.add(new HydrationEntry(stack, values));
        }
    }
}
