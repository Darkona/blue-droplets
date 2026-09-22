package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.compat.create.CreateCompat;
import com.darkona.dropletsofthirst.compat.jei.HydrationEntry;
import com.darkona.dropletsofthirst.compat.jei.PurificationEntry;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.registry.ItemInit;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.darkona.dropletsofthirst.gametest.TestSupport.assertValueEqual;

/**
 * What the recipe viewer pages (JEI, EMI) show. The pages are built from plain data that the server can build too,
 * so the content is checked here without a client.
 */
@GameTestHolder(DropletsOfThirst.ID)
@PrefixGameTestTemplate(false)
public class ViewerTests
{
    @GameTest(template = "empty")
    public static void purificationPageShowsCauldronsAndSandFilter(GameTestHelper helper)
    {
        List<PurificationEntry> entries = PurificationEntry.all();
        PurificationEntry cauldron = entry(helper, entries, "cauldron");
        PurificationEntry heated = entry(helper, entries, "heated_cauldron");
        assertValueEqual(helper, cauldron.inputs().size(), cauldron.outputs().size(), "cauldron inputs and outputs cycle together");
        for (ItemStack output : cauldron.outputs())
            assertValueEqual(helper, WaterPurity.getPurity(output), WaterPurity.CAULDRON_PURITY, "purity out of a cauldron");
        for (ItemStack output : heated.outputs())
            assertValueEqual(helper, WaterPurity.getPurity(output), WaterPurity.HEATED_CAULDRON_PURITY, "purity out of a heated cauldron");
        helper.assertTrue(heated.below().stream().anyMatch(stack -> stack.is(Items.CAMPFIRE)), "a campfire heats the cauldron");
        long filters = entries.stream().filter(entry -> entry.method().equals("sand_filter")).count();
        assertValueEqual(helper, filters > 0, CreateCompat.LOADED, "Sand Filter entries with Create installed");
        for (PurificationEntry entry : entries)
            if (entry.method().equals("sand_filter"))
                helper.assertTrue(WaterPurity.getPurity(entry.fluidOut()) > WaterPurity.getPurity(entry.fluidIn()), "a Sand Filter entry that does not purify");
        helper.succeed();
    }

    private static PurificationEntry entry(GameTestHelper helper, List<PurificationEntry> entries, String method)
    {
        PurificationEntry found = entries.stream().filter(entry -> entry.method().equals(method)).findFirst().orElse(null);
        helper.assertTrue(found != null, "no " + method + " entry on the purification page");
        return found;
    }

    @GameTest(template = "empty")
    public static void hydrationPageListsDrinksWithTheirValues(GameTestHelper helper)
    {
        List<HydrationEntry> entries = HydrationEntry.all();
        HydrationEntry bottle = entries.stream().filter(entry -> entry.stack().is(Items.POTION)).findFirst().orElse(null);
        helper.assertTrue(bottle != null, "no water bottle on the hydration page");
        helper.assertTrue(PotionUtils.getPotion(bottle.stack()) == Potions.WATER, "the potion entry is not a water bottle");
        assertValueEqual(helper, bottle.values().thirst(), 4, "water bottle thirst on the hydration page");
        assertValueEqual(helper, bottle.values().quenched(), 5, "water bottle quenched on the hydration page");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.stack().is(ItemInit.TERRACOTTA_WATER_BOWL.get())), "no terracotta water bowl on the hydration page");
        for (HydrationEntry entry : entries)
            helper.assertTrue(entry.values().thirst() != 0 || entry.values().quenched() != 0, entry.stack() + " does nothing and is on the hydration page");
        helper.succeed();
    }
}
