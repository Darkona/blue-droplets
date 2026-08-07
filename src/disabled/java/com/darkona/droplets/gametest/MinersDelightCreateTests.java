package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.purity.WaterPurity;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import net.createmod.catnip.data.Pair;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.droplets.gametest.MinersDelightTests.item;

/**
 * Miner's Delight with Create ({@code -PwithDelight -PwithCompat}): Create fills and empties the water cup with its
 * purity, through the recipes of this mod (the ones Miner's Delight ships sit in a folder 1.21 does not read).
 */
@PrefixGameTestTemplate(false)
public class MinersDelightCreateTests
{
    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void spoutFillsTheCupWithThePurityOfTheWater(GameTestHelper helper)
    {
        for (int purity = WaterPurity.MIN_PURITY; purity <= WaterPurity.MAX_PURITY; purity++)
        {
            FluidStack water = WaterPurity.addPurity(new FluidStack(Fluids.WATER, 2000), purity);
            ItemStack cup = FillingBySpout.fillItem(helper.getLevel(), 1000, new ItemStack(item("copper_cup")), water);
            helper.assertTrue(cup.is(item("water_cup")), "a spout filled the copper cup into " + cup);
            helper.assertTrue(WaterPurity.hasPurity(cup), "a cup filled by a spout with water of purity " + purity + " has none");
            helper.assertValueEqual(WaterPurity.getPurity(cup), purity, "purity of a cup filled by a spout");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void emptyingTheCupKeepsThePurity(GameTestHelper helper)
    {
        for (int purity = WaterPurity.MIN_PURITY; purity <= WaterPurity.MAX_PURITY; purity++)
        {
            ItemStack cup = WaterPurity.addPurity(new ItemStack(item("water_cup")), purity);
            Pair<FluidStack, ItemStack> emptied = GenericItemEmptying.emptyItem(helper.getLevel(), cup, true);
            helper.assertTrue(emptied.getSecond().is(item("copper_cup")), "emptying the water cup gave " + emptied.getSecond());
            helper.assertValueEqual(emptied.getFirst().getAmount(), 1000, "water emptied from a cup");
            helper.assertTrue(WaterPurity.hasPurity(emptied.getFirst()), "the water from a cup of purity " + purity + " has none");
            helper.assertValueEqual(WaterPurity.getPurity(emptied.getFirst()), purity, "purity of water emptied from a cup");
        }
        helper.assertFalse(WaterPurity.hasPurity(GenericItemEmptying.emptyItem(helper.getLevel(), new ItemStack(item("water_cup")), true).getFirst()),
                "the emptying recipe kept the purity of an earlier cup");
        helper.succeed();
    }
}
