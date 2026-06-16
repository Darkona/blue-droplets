package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.compat.coldsweat.ColdSweatCompat;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.core.NumberRows;
import com.darkona.droplets.foundation.config.CompatConfig;
import com.darkona.droplets.foundation.config.GameplayConfig;
import com.momosoftworks.coldsweat.core.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.droplets.gametest.TestSupport.player;

/**
 * Cold Sweat: its waterskin as a water drink with purity, drinking water cools, body temperature drives the climate
 * multiplier. Registered by {@link DropletsGameTests} only when Cold Sweat is installed.
 */
@PrefixGameTestTemplate(false)
public class ColdSweatTests
{
    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void waterskinIsAWaterDrinkThatCooks(GameTestHelper helper)
    {
        ItemStack waterskin = new ItemStack(ModItems.FILLED_WATERSKIN.get());
        ThirstValues values = ThirstHelper.valuesOf(waterskin);
        helper.assertTrue(values != null && values.thirst() == 6 && values.quenched() == 3, "filled waterskin drink values " + values);
        helper.assertTrue(WaterPurity.isWaterFilledContainer(waterskin), "the filled waterskin is not a water container");
        ItemStack dirty = WaterPurity.addPurity(waterskin.copy(), 0);
        ItemStack cooked = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(dirty), helper.getLevel())
                .map(recipe -> recipe.value().assemble(new SingleRecipeInput(dirty), helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        helper.assertTrue(cooked.is(ModItems.FILLED_WATERSKIN), "a furnace does not purify a dirty waterskin");
        helper.assertValueEqual(WaterPurity.getPurity(cooked), 2, "purity of a dirty waterskin out of a furnace");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void waterskinTakesThePurityOfACauldron(GameTestHelper helper)
    {
        BlockPos cauldron = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(cauldron.below(), Blocks.CAMPFIRE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        ServerPlayer player = player(helper);
        ItemStack empties = new ItemStack(ModItems.WATERSKIN.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, empties);
        player.gameMode.useItemOn(player, helper.getLevel(), empties, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(cauldron), Direction.UP, cauldron, false));
        ItemStack filled = filledWaterskin(player);
        helper.assertTrue(!filled.isEmpty(), "no waterskin was filled from the cauldron");
        helper.assertValueEqual(WaterPurity.getPurity(filled), WaterPurity.HEATED_CAULDRON_PURITY, "purity of a waterskin filled from a cauldron on a campfire");
        helper.succeedWhen(() -> helper.assertFalse(WaterPurity.hasPurity(empties), "the empty waterskins kept a purity"));
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void waterskinTakesThePurityOfWorldWater(GameTestHelper helper)
    {
        BlockPos water = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        ServerPlayer player = player(helper);
        player.moveTo(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 0, 90);
        ItemStack empty = new ItemStack(ModItems.WATERSKIN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, empty);
        player.gameMode.useItem(player, helper.getLevel(), empty, InteractionHand.MAIN_HAND);
        ItemStack filled = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(filled.is(ModItems.FILLED_WATERSKIN), "no waterskin was filled from the water source, got " + filled);
        helper.assertTrue(WaterPurity.hasPurity(filled), "a waterskin filled from the world has no purity");
        helper.assertValueEqual(WaterPurity.getPurity(filled), WaterPurity.getWaterPurity(helper.getLevel(), water, true), "purity of a waterskin filled from the world");
        helper.succeed();
    }

    private static ItemStack filledWaterskin(ServerPlayer player)
    {
        for (ItemStack stack : player.getInventory().items)
            if (stack.is(ModItems.FILLED_WATERSKIN))
                return stack;
        return ItemStack.EMPTY;
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void drinkingWaterCoolsAndBodyTemperatureDrivesThirst(GameTestHelper helper)
    {
        double cooling = CompatConfig.COLD_SWEAT_DRINK_COOLING.get();
        int ticks = CompatConfig.COLD_SWEAT_DRINK_COOLING_TICKS.get();
        try
        {
            CompatConfig.COLD_SWEAT_DRINK_COOLING.set(50.0);
            CompatConfig.COLD_SWEAT_DRINK_COOLING_TICKS.set(0);
            ServerPlayer player = player(helper);
            double before = ColdSweatCompat.bodyTemperature(player);
            PlayerThirst.drink(player, PotionContents.createItemStack(Items.POTION, Potions.WATER), 6, 8, 2);
            double after = ColdSweatCompat.bodyTemperature(player);
            helper.assertTrue(Math.abs(after - (before - 50)) < 0.001, "body temperature " + before + " -> " + after + " after a cooling drink of 50");

            PlayerThirst.drink(player, new ItemStack(ModItems.FILLED_WATERSKIN.get()), 6, 3, 2);
            helper.assertTrue(Math.abs(ColdSweatCompat.bodyTemperature(player) - after) < 0.001, "the waterskin cooled as well as its own temperature");

            double expected = GameplayConfig.DEPLETION_MULTIPLIER.get() * NumberRows.curve(NumberRows.parse(CompatConfig.COLD_SWEAT_BODY_TEMPERATURE_CURVE.get(), 2), after);
            helper.assertTrue(Math.abs(ThirstHelper.getExhaustionBiomeModifier(player) - expected) < 0.001,
                    "climate multiplier " + ThirstHelper.getExhaustionBiomeModifier(player) + " at body temperature " + after + ", expected " + expected);
        }
        finally
        {
            CompatConfig.COLD_SWEAT_DRINK_COOLING.set(cooling);
            CompatConfig.COLD_SWEAT_DRINK_COOLING_TICKS.set(ticks);
        }
        helper.succeed();
    }
}
