package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.foundation.common.event.Events;
import com.darkona.droplets.api.PurityLevel;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;

import static com.darkona.droplets.gametest.TestSupport.player;

import static com.darkona.droplets.gametest.TestSupport.assertValueEqual;
import static com.darkona.droplets.gametest.TestSupport.assertTrue;
import static com.darkona.droplets.gametest.TestSupport.assertFalse;

/**
 * Farmer's Delight addons ({@code -PwithDelight}): the clean water rule of the {@code clean_water_cooking} pack.
 * Registered by {@link DropletsGameTests} only when Farmer's Delight and Brewin' and Chewin' are installed.
 */
@PrefixGameTestTemplate(false)
public class DelightTests
{
    private static final int KETTLE_WATER_SLOT = 6;

    private static ItemStack waterBottle(int purity)
    {
        return WaterPurity.addPurity(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), purity);
    }

    private static Block block(String id)
    {
        return Registry.BLOCK.get(new ResourceLocation(id));
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos relative)
    {
        BlockPos pos = helper.absolutePos(relative);
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    /**
     * The clean_water_cooking pack: Farmer's Delight's cooking pot and wheat dough take water of purity 3 or better;
     * water without a purity counts as acceptable.
     */
    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void cookingRecipesNeedCleanWater(GameTestHelper helper)
    {
        waterRule(helper, "farmersdelight:cooking_pot", PurityLevel.ACCEPTABLE.level());
        waterRule(helper, "farmersdelight:wheat_dough_from_water", PurityLevel.ACCEPTABLE.level());
        helper.succeed();
    }

    /**
     * The water ingredient of {@code id} (the one that takes a water bucket) accepts buckets without a purity and of
     * {@code min} or better, and refuses dirtier ones.
     */
    private static void waterRule(GameTestHelper helper, String id, int min)
    {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(new ResourceLocation(id)).orElse(null);
        assertTrue(helper, recipe != null, "no recipe " + id);
        Ingredient water = recipe.getIngredients().stream().filter(ingredient -> ingredient.test(new ItemStack(Items.WATER_BUCKET))).findFirst().orElse(null);
        assertTrue(helper, water != null, id + " takes no water bucket");
        for (int purity = WaterPurity.MIN_PURITY; purity <= WaterPurity.MAX_PURITY; purity++)
            assertValueEqual(helper, water.test(WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), purity)), purity >= min, id + " takes water of purity " + purity);
    }
}
