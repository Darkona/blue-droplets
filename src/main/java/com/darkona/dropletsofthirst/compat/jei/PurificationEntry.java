package com.darkona.dropletsofthirst.compat.jei;

import com.darkona.dropletsofthirst.compat.create.CreateCompat;
import com.darkona.dropletsofthirst.content.data.DropletsTags;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.registry.ItemInit;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A way of purifying water that is not a recipe, for the recipe viewer's purification category: the water cauldron
 * (plain and on a heat source) and, with Create, the Sand Filter. Cooking recipes and Create's own recipes are shown by
 * the viewer in their usual categories. Plain data, no recipe viewer classes, so the server can build it too.
 *
 * @param method    {@code cauldron}, {@code heated_cauldron} or {@code sand_filter}: the last part of the text key
 * @param inputs    water containers going in, one per output (the viewer cycles them together)
 * @param outputs   water containers coming out
 * @param fluidIn   water going in, or empty
 * @param fluidOut  water coming out, or empty
 * @param machine   the block doing it
 * @param below     blocks that must be under {@code machine}, or none
 */
public record PurificationEntry(String method, List<ItemStack> inputs, List<ItemStack> outputs, FluidStack fluidIn, FluidStack fluidOut,
                                ItemStack machine, List<ItemStack> below)
{
    public static final int FLUID_AMOUNT = 1000;

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
        Item filter = CreateCompat.sandFilter();
        if (filter != Items.AIR)
            for (int purity = WaterPurity.MIN_PURITY; purity < WaterPurity.MAX_PURITY; purity++)
            {
                int filtered = CreateCompat.sandFilterPurity(purity);
                if (filtered > purity)
                    entries.add(new PurificationEntry("sand_filter", List.of(), List.of(), water(purity), water(filtered), new ItemStack(filter), List.of()));
            }
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
        return new PurificationEntry(method, inputs, outputs, FluidStack.EMPTY, FluidStack.EMPTY, machine, below);
    }

    /**
     * The containers a water cauldron fills: bottle, bucket and terracotta bowl.
     */
    private static List<ItemStack> waterContainers()
    {
        return List.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), new ItemStack(Items.WATER_BUCKET), new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()));
    }

    /**
     * Items of the blocks in {@code droplets_of_thirst:cauldron_heat_sources} (fire and lava have none).
     */
    private static List<ItemStack> heatSources()
    {
        List<ItemStack> items = new ArrayList<>();
        for (Holder<Block> block : Registry.BLOCK.getTagOrEmpty(DropletsTags.CAULDRON_HEAT_SOURCES))
        {
            Item item = block.value().asItem();
            if (item != Items.AIR)
                items.add(new ItemStack(item));
        }
        return items;
    }

    private static FluidStack water(int purity)
    {
        return WaterPurity.addPurity(new FluidStack(Fluids.WATER, FLUID_AMOUNT), purity);
    }
}
