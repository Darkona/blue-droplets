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
import com.momosoftworks.coldsweat.util.registries.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.darkona.droplets.gametest.TestSupport.player;

import static com.darkona.droplets.gametest.TestSupport.assertValueEqual;
import static com.darkona.droplets.gametest.TestSupport.assertTrue;
import static com.darkona.droplets.gametest.TestSupport.assertFalse;

/**
 * Cold Sweat: its waterskin as a water drink with purity, drinking water cools, body temperature drives the climate
 * multiplier. Registered by {@link DropletsGameTests} only when Cold Sweat is installed.
 */
@PrefixGameTestTemplate(false)
public class ColdSweatTests
{
    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void emptiedWaterskinLosesItsPurity(GameTestHelper helper)
    {
        ItemStack filled = WaterPurity.addPurity(new ItemStack(ModItems.FILLED_WATERSKIN), 0);
        ItemStack empty = filled.getContainerItem();
        assertTrue(helper, empty.is(ModItems.WATERSKIN), "the remainder is not an empty waterskin");
        assertFalse(helper, WaterPurity.hasPurity(empty), "the emptied waterskin kept its purity");
        assertTrue(helper, ItemStack.isSameItemSameTags(empty, new ItemStack(ModItems.WATERSKIN)), "the emptied waterskin does not stack with a new one");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void waterskinIsAWaterDrinkThatCooks(GameTestHelper helper)
    {
        ItemStack waterskin = new ItemStack(ModItems.FILLED_WATERSKIN);
        ThirstValues values = ThirstHelper.valuesOf(waterskin);
        assertTrue(helper, values != null && values.thirst() == 6 && values.quenched() == 3, "filled waterskin drink values " + values);
        assertTrue(helper, WaterPurity.isWaterFilledContainer(waterskin), "the filled waterskin is not a water container");
        ItemStack dirty = WaterPurity.addPurity(waterskin.copy(), 0);
        ItemStack cooked = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(dirty), helper.getLevel())
                .map(recipe -> recipe.assemble(new SimpleContainer(dirty))).orElse(ItemStack.EMPTY);
        assertTrue(helper, cooked.is(ModItems.FILLED_WATERSKIN), "a furnace does not purify a dirty waterskin");
        assertValueEqual(helper, WaterPurity.getPurity(cooked), 2, "purity of a dirty waterskin out of a furnace");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void waterskinTakesThePurityOfACauldron(GameTestHelper helper)
    {
        BlockPos cauldron = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(cauldron.below(), Blocks.CAMPFIRE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        ServerPlayer player = player(helper);
        ItemStack empties = new ItemStack(ModItems.WATERSKIN, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, empties);
        player.gameMode.useItemOn(player, helper.getLevel(), empties, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(cauldron), Direction.UP, cauldron, false));
        ItemStack filled = filledWaterskin(player);
        assertTrue(helper, !filled.isEmpty(), "no waterskin was filled from the cauldron");
        assertValueEqual(helper, WaterPurity.getPurity(filled), WaterPurity.HEATED_CAULDRON_PURITY, "purity of a waterskin filled from a cauldron on a campfire");
        helper.succeedWhen(() -> assertFalse(helper, WaterPurity.hasPurity(empties), "the empty waterskins kept a purity"));
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void waterskinTakesThePurityOfWorldWater(GameTestHelper helper)
    {
        BlockPos water = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        ServerPlayer player = player(helper);
        player.moveTo(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 0, 90);
        ItemStack empty = new ItemStack(ModItems.WATERSKIN);
        player.setItemInHand(InteractionHand.MAIN_HAND, empty);
        player.gameMode.useItem(player, helper.getLevel(), empty, InteractionHand.MAIN_HAND);
        ItemStack filled = player.getItemInHand(InteractionHand.MAIN_HAND);
        assertTrue(helper, filled.is(ModItems.FILLED_WATERSKIN), "no waterskin was filled from the water source, got " + filled);
        assertTrue(helper, WaterPurity.hasPurity(filled), "a waterskin filled from the world has no purity");
        assertValueEqual(helper, WaterPurity.getPurity(filled), WaterPurity.getWaterPurity(helper.getLevel(), water, true), "purity of a waterskin filled from the world");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void waterskinTakesThePurityOfATank(GameTestHelper helper)
    {
        Block tankBlock = Registry.BLOCK.get(new ResourceLocation("create", "fluid_tank"));
        if (tankBlock == Blocks.AIR)
        {
            helper.succeed();
            return;
        }
        BlockPos tank = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(tank, tankBlock.defaultBlockState());
        IFluidHandler handler = helper.getLevel().getBlockEntity(tank).getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, Direction.UP).orElse(null);
        assertTrue(helper, handler != null, "the tank has no fluid handler");
        handler.fill(WaterPurity.addPurity(new FluidStack(Fluids.WATER, 1000), 0), IFluidHandler.FluidAction.EXECUTE);
        ServerPlayer player = player(helper);
        ItemStack empties = new ItemStack(ModItems.WATERSKIN, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, empties);
        player.gameMode.useItemOn(player, helper.getLevel(), empties, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(tank), Direction.UP, tank, false));
        ItemStack filled = filledWaterskin(player);
        assertTrue(helper, !filled.isEmpty(), "no waterskin was filled from the tank");
        assertValueEqual(helper, WaterPurity.getPurity(filled), 0, "purity of a waterskin filled from a tank of dirty water");
        assertFalse(helper, WaterPurity.hasPurity(empties), "the empty waterskins got a purity");
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
            TestSupport.set(CompatConfig.COLD_SWEAT_DRINK_COOLING, 50.0);
            TestSupport.set(CompatConfig.COLD_SWEAT_DRINK_COOLING_TICKS, 0);
            ServerPlayer player = player(helper);
            double before = ColdSweatCompat.bodyTemperature(player);
            PlayerThirst.drink(player, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), 6, 8, 2);
            double after = ColdSweatCompat.bodyTemperature(player);
            assertTrue(helper, Math.abs(after - (before - 50)) < 0.001, "body temperature " + before + " -> " + after + " after a cooling drink of 50");

            PlayerThirst.drink(player, new ItemStack(ModItems.FILLED_WATERSKIN), 6, 3, 2);
            assertTrue(helper, Math.abs(ColdSweatCompat.bodyTemperature(player) - after) < 0.001, "the waterskin cooled as well as its own temperature");

            double expected = GameplayConfig.DEPLETION_MULTIPLIER.get() * NumberRows.curve(NumberRows.parse(CompatConfig.COLD_SWEAT_BODY_TEMPERATURE_CURVE.get(), 2), after);
            assertTrue(helper, Math.abs(ThirstHelper.getExhaustionBiomeModifier(player) - expected) < 0.001,
                    "climate multiplier " + ThirstHelper.getExhaustionBiomeModifier(player) + " at body temperature " + after + ", expected " + expected);
        }
        finally
        {
            TestSupport.set(CompatConfig.COLD_SWEAT_DRINK_COOLING, cooling);
            TestSupport.set(CompatConfig.COLD_SWEAT_DRINK_COOLING_TICKS, ticks);
        }
        helper.succeed();
    }
}
