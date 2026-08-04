package com.darkona.droplets.compat.jei;

import com.darkona.droplets.content.data.DropletsTags;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ItemInit;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * A way of purifying water that is not a recipe, for the recipe viewer's purification category: the water cauldron
 * (plain and on a heat source). Cooking recipes are shown by the viewer in their usual categories. Plain data, no recipe viewer classes, so the server can build it too.
 *
 * @param method    {@code cauldron} or {@code heated_cauldron}: the last part of the text key
 * @param inputs    water containers going in, one per output (the viewer cycles them together)
 * @param outputs   water containers coming out
 * @param machine   the block doing it
 * @param below     blocks that must be under {@code machine}, or none
 */
public record PurificationEntry(String method, List<ItemStack> inputs, List<ItemStack> outputs, ItemStack machine, List<ItemStack> below)
{
    /**
     * Every entry, in display order; none with {@code purity.enabled=false}, so the category is not shown then.
     */
    public static List<PurificationEntry> all()
    {
        if (!WaterPurity.enabled())
            return List.of();
        List<PurificationEntry> entries = new ArrayList<>();
        ItemStack cauldron = new ItemStack(Items.CAULDRON);
        entries.add(containers("cauldron", WaterPurity.CAULDRON_PURITY, cauldron, List.of()));
        List<ItemStack> heat = heatSources();
        if (!heat.isEmpty())
            entries.add(containers("heated_cauldron", WaterPurity.HEATED_CAULDRON_PURITY, cauldron, heat));
        return entries;
    }

    /**
     * Water containers of every purity below {@code purity} in, the same containers at {@code purity} out.
     */
    private static PurificationEntry containers(String method, int purity, ItemStack machine, List<ItemStack> below)
    {
        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        for (int from = WaterPurity.MIN_PURITY; from < purity; from++)
            for (ItemStack container : waterContainers())
            {
                inputs.add(WaterPurity.addPurity(container.copy(), from));
                outputs.add(WaterPurity.addPurity(container.copy(), purity));
            }
        return new PurificationEntry(method, inputs, outputs, machine, below);
    }

    /**
     * The containers a water cauldron fills: bottle, bucket and terracotta bowl.
     */
    private static List<ItemStack> waterContainers()
    {
        return List.of(PotionContents.createItemStack(Items.POTION, Potions.WATER), new ItemStack(Items.WATER_BUCKET), new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()));
    }

    /**
     * Items of the blocks in {@code blue_droplets:cauldron_heat_sources} (fire and lava have none).
     */
    private static List<ItemStack> heatSources()
    {
        List<ItemStack> items = new ArrayList<>();
        for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(DropletsTags.CAULDRON_HEAT_SOURCES))
        {
            Item item = block.value().asItem();
            if (item != Items.AIR)
                items.add(new ItemStack(item));
        }
        return items;
    }
}
