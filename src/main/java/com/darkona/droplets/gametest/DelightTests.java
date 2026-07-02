package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.PurityLevel;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.CompatConfig;
import com.lance5057.extradelight.util.BottleFluidRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.satisfy.farm_and_charm.core.block.TimberWellBlock;
import net.satisfy.herbalbrews.core.blocks.entity.TeaKettleBlockEntity;

import java.util.Arrays;

import static com.darkona.droplets.gametest.TestSupport.player;

/**
 * Farmer's Delight addons and Let's Do mods ({@code -PwithDelight}): kettles refuse dirty water, the infinite water
 * sources give the world's purity, Extra Delight bottles keep theirs. Registered by {@link DropletsGameTests} only when
 * Extra Delight, Farm & Charm, HerbalBrews and Brewery are all installed.
 */
@PrefixGameTestTemplate(false)
public class DelightTests
{
    private static final int KETTLE_WATER_SLOT = 6;

    private static ItemStack waterBottle(int purity)
    {
        return WaterPurity.addPurity(PotionContents.createItemStack(Items.POTION, Potions.WATER), purity);
    }

    private static Block block(String id)
    {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos relative)
    {
        BlockPos pos = helper.absolutePos(relative);
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void extraDelightBottlesKeepTheirPurity(GameTestHelper helper)
    {
        FluidStack fromDirty = BottleFluidRegistry.getFluidFromBottle(waterBottle(0));
        helper.assertTrue(fromDirty.is(Fluids.WATER) && fromDirty.getAmount() == 250, "a water bottle is not 250 mB of water");
        helper.assertValueEqual(WaterPurity.hasPurity(fromDirty) ? WaterPurity.getPurity(fromDirty) : -1, 0, "purity of the water from a dirty bottle");
        helper.assertFalse(WaterPurity.hasPurity(BottleFluidRegistry.getFluidFromBottle(PotionContents.createItemStack(Items.POTION, Potions.WATER))),
                "the registry's own water stack took the purity of an earlier bottle");

        ItemStack pure = BottleFluidRegistry.getBottleFromFluid(WaterPurity.addPurity(new FluidStack(Fluids.WATER, 1000), WaterPurity.MAX_PURITY));
        helper.assertTrue(WaterPurity.isWaterFilledContainer(pure), "250 mB of water is not a water bottle");
        helper.assertValueEqual(WaterPurity.hasPurity(pure) ? WaterPurity.getPurity(pure) : -1, WaterPurity.MAX_PURITY, "purity of a bottle of pure water");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void extraDelightTapsGiveTheWorldPurity(GameTestHelper helper)
    {
        BlockPos tap = new BlockPos(1, 2, 1);
        helper.setBlock(tap, block("extradelight:tap"));
        int expected = WaterPurity.getWaterPurity(helper.getLevel(), helper.absolutePos(tap), true);

        IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(tap), null);
        helper.assertTrue(tank != null, "the tap has no fluid handler");
        FluidStack drained = tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        helper.assertValueEqual(WaterPurity.hasPurity(drained) ? WaterPurity.getPurity(drained) : -1, expected, "purity of water drained from a tap");
        helper.assertValueEqual(WaterPurity.hasPurity(tank.getFluidInTank(0)) ? WaterPurity.getPurity(tank.getFluidInTank(0)) : -1, expected, "purity of the water a tap shows");

        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        helper.getBlockState(tap).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper, tap));
        ItemStack bottle = player.getInventory().items.stream().filter(WaterPurity::isWaterFilledContainer).findFirst().orElse(ItemStack.EMPTY);
        helper.assertFalse(bottle.isEmpty(), "no water bottle from the tap");
        helper.assertValueEqual(WaterPurity.hasPurity(bottle) ? WaterPurity.getPurity(bottle) : -1, expected, "purity of a bottle filled at a tap");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void farmAndCharmWellGivesTheWorldPurity(GameTestHelper helper)
    {
        BlockPos well = new BlockPos(1, 2, 1);
        helper.setBlock(well, block("farm_and_charm:timber_well").defaultBlockState().setValue(TimberWellBlock.LEVEL, 3));
        int expected = WaterPurity.getWaterPurity(helper.getLevel(), helper.absolutePos(well), true);

        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.getBlockState(well).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper, well));
        ItemStack bucket = player.getMainHandItem();
        helper.assertTrue(bucket.is(Items.WATER_BUCKET), "no water bucket from the well: " + bucket);
        helper.assertValueEqual(WaterPurity.hasPurity(bucket) ? WaterPurity.getPurity(bucket) : -1, expected, "purity of a bucket filled at the well");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void farmAndCharmSinkIsHooked(GameTestHelper helper)
    {
        // Farm & Charm has the sink class but other Let's Do mods register the block, so this only checks that the
        // hook applies to it.
        try
        {
            Class<?> sink = Class.forName("net.satisfy.farm_and_charm.core.block.SinkBlock");
            helper.assertTrue(Arrays.stream(sink.getDeclaredMethods()).anyMatch(method -> method.getName().endsWith("blue_droplets$worldPurity")), "the sink hook is not applied");
        }
        catch (ClassNotFoundException e)
        {
            helper.fail("no Farm & Charm sink class");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void teaKettleTakesWaterOfPurityTwoOrMore(GameTestHelper helper)
    {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, block("herbalbrews:tea_kettle"));
        TeaKettleBlockEntity kettle = helper.getBlockEntity(pos);
        int min = CompatConfig.KETTLE_MIN_PURITY.get();
        helper.assertValueEqual(min, PurityLevel.MURKY.level(), "default kettleMinPurity");
        for (int purity = WaterPurity.MIN_PURITY; purity < min; purity++)
        {
            kettle.setItem(KETTLE_WATER_SLOT, waterBottle(purity));
            kettle.tick(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos));
            helper.assertValueEqual(kettle.getWaterLevel(), 0, "water level after water of purity " + purity);
            helper.assertTrue(WaterPurity.isWaterFilledContainer(kettle.getItem(KETTLE_WATER_SLOT)), "water of purity " + purity + " left the slot");
        }

        for (int purity = min; purity <= WaterPurity.MAX_PURITY; purity++)
        {
            int before = kettle.getWaterLevel();
            kettle.setItem(KETTLE_WATER_SLOT, waterBottle(purity));
            kettle.tick(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos));
            helper.assertValueEqual(kettle.getWaterLevel(), before + 25, "water level after water of purity " + purity);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void breweryKettleRefusesDirtyWater(GameTestHelper helper)
    {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, block("brewery:wooden_brewingstation"));
        ServerPlayer player = player(helper);
        for (int purity = WaterPurity.MIN_PURITY; purity <= WaterPurity.MAX_PURITY; purity++)
        {
            player.setItemInHand(InteractionHand.MAIN_HAND, WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), purity));
            PlayerInteractEvent.RightClickBlock event = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, helper.absolutePos(pos), hit(helper, pos)));
            helper.assertValueEqual(event.isCanceled(), purity < CompatConfig.KETTLE_MIN_PURITY.get(), "brewing station refuses water of purity " + purity);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.assertFalse(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, helper.absolutePos(pos), hit(helper, pos))).isCanceled(),
                "brewing station refuses water without a purity");
        helper.succeed();
    }
}
