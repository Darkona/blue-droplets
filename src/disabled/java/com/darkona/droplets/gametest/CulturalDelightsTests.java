package com.darkona.droplets.gametest;

import com.baisylia.culturaldelights.recipes.VatRecipe;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.purity.WaterPurity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/**
 * Cultural Delights ({@code -PwithDelight}): the vat holds no fluid and hands out no water, water only goes in as
 * buckets in its aging recipes, so its purity rule lives in those recipes (the {@code clean_water_cooking} pack).
 */
@PrefixGameTestTemplate(false)
public class CulturalDelightsTests
{
    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void coldAgingRecipesNeedAcceptableWater(GameTestHelper helper)
    {
        VatRecipe beer = (VatRecipe) helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("culturaldelights:aging/fermenting/beer")).orElseThrow().value();
        for (int purity = WaterPurity.MIN_PURITY; purity <= WaterPurity.MAX_PURITY; purity++)
        {
            ItemStackHandler slots = new ItemStackHandler(9);
            slots.setStackInSlot(0, WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), purity));
            slots.setStackInSlot(1, new ItemStack(Items.WHEAT));
            slots.setStackInSlot(2, new ItemStack(Items.WHEAT));
            slots.setStackInSlot(3, new ItemStack(Items.SUGAR));
            slots.setStackInSlot(4, new ItemStack(Items.BROWN_MUSHROOM));
            slots.setStackInSlot(6, new ItemStack(Items.GLASS_BOTTLE));
            helper.assertValueEqual(beer.matches(new RecipeWrapper(slots), helper.getLevel()), purity >= 3, "beer with a water bucket of purity " + purity);
        }
        helper.succeed();
    }
}
