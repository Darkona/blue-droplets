package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.create.CreateRegistry;
import com.darkona.droplets.compat.create.SandFilterBlock;
import com.darkona.droplets.compat.create.SandFilterBlockEntity;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.CompatConfig;
import com.darkona.droplets.foundation.tab.ThirstTab;
import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Create: open pipe ends and the Sand Filter. Registered by {@link DropletsGameTests} only when Create is installed.
 */
@PrefixGameTestTemplate(false)
public class CreateTests
{
    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void openPipeKeepsCauldronPurity(GameTestHelper helper)
    {
        BlockPos cauldron = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockState state = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3).setValue(WaterPurity.BLOCK_PURITY, 1);
        helper.getLevel().setBlockAndUpdate(cauldron, state);
        FluidStack drained = drainThroughPipe(helper, cauldron.above());
        helper.assertFalse(drained.isEmpty(), "the pipe drained nothing from the cauldron");
        helper.assertValueEqual(WaterPurity.getPurity(drained), 0, "purity of water drained from a dirty cauldron");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void openPipeKeepsWorldPurity(GameTestHelper helper)
    {
        BlockPos water = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        int expected = WaterPurity.getWaterPurity(helper.getLevel(), water, true);
        FluidStack drained = drainThroughPipe(helper, water.above());
        helper.assertFalse(drained.isEmpty(), "the pipe drained nothing from the water source");
        helper.assertTrue(WaterPurity.hasPurity(drained), "water drained from the world has no purity");
        helper.assertValueEqual(WaterPurity.getPurity(drained), expected, "purity of water drained from the world");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void sandFilterTurnsWithItsFacing(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, CreateRegistry.SAND_FILTER_BLOCK.get().defaultBlockState().setValue(SandFilterBlock.FACING, Direction.EAST));
        IFluidHandler out = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.EAST);
        IFluidHandler in = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.WEST);
        helper.assertTrue(out != null && in != null && out != in, "an east-facing filter has its tanks east and west");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP) == null, "an east-facing filter has no tank on top");
        helper.assertValueEqual(in.fill(water(0, 1000), IFluidHandler.FluidAction.EXECUTE), 1000, "water accepted on the input side");
        helper.assertValueEqual(out.fill(water(0, 1000), IFluidHandler.FluidAction.SIMULATE), 0, "water accepted on the output side");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID, timeoutTicks = 400)
    public static void sandFiltersInARowPurifyInStages(GameTestHelper helper)
    {
        BlockPos top = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos bottom = top.below();
        BlockState filter = CreateRegistry.SAND_FILTER_BLOCK.get().defaultBlockState();
        helper.getLevel().setBlockAndUpdate(top, filter);
        helper.getLevel().setBlockAndUpdate(bottom, filter);
        IFluidHandler in = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, top, Direction.UP);
        in.fill(water(0, 1000), IFluidHandler.FluidAction.EXECUTE);
        int expected = SandFilterBlockEntity.filteredPurity(SandFilterBlockEntity.filteredPurity(0));
        helper.succeedWhen(() -> {
            FluidStack out = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, bottom, Direction.DOWN).getFluidInTank(0);
            helper.assertValueEqual(out.getAmount(), 1000, "water out of the second filter");
            helper.assertValueEqual(WaterPurity.getPurity(out), expected, "purity after two filters");
        });
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void sandFilterStopsAtMaxPurity(GameTestHelper helper)
    {
        int max = CompatConfig.SAND_FILTER_MAX_PURITY.get();
        try
        {
            CompatConfig.SAND_FILTER_MAX_PURITY.set(1);
            helper.assertValueEqual(SandFilterBlockEntity.filteredPurity(0), 1, "dirty water with sandFilterMaxPurity 1");
            helper.assertValueEqual(SandFilterBlockEntity.filteredPurity(1), 1, "slightly dirty water with sandFilterMaxPurity 1");
            helper.assertValueEqual(SandFilterBlockEntity.filteredPurity(3), 3, "purified water is never made dirtier");
        }
        finally
        {
            CompatConfig.SAND_FILTER_MAX_PURITY.set(max);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void sandFilterIsInCreateAndOwnCreativeTabs(GameTestHelper helper)
    {
        CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
        for (CreativeModeTab tab : new CreativeModeTab[]{AllCreativeModeTabs.BASE_CREATIVE_TAB.get(), ThirstTab.THIRST_TAB.get()})
        {
            tab.buildContents(parameters);
            helper.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(CreateRegistry.SAND_FILTER_BLOCK.asItem())), "Sand Filter missing from " + tab.getDisplayName().getString());
        }
        helper.succeed();
    }

    private static FluidStack water(int purity, int amount)
    {
        return WaterPurity.addPurity(new FluidStack(Fluids.WATER, amount), purity);
    }

    /**
     * An open pipe end at {@code pipePos} facing down, drained like a pump would.
     */
    private static FluidStack drainThroughPipe(GameTestHelper helper, BlockPos pipePos)
    {
        OpenEndedPipe pipe = new OpenEndedPipe(new BlockFace(pipePos, Direction.DOWN));
        pipe.manageSource(helper.getLevel(), null);
        IFluidHandler handler = pipe.provideHandler().getCapability();
        return handler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
    }
}
