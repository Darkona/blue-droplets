package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.compat.create.CreateRegistry;
import com.darkona.dropletsofthirst.compat.create.SandFilterBlock;
import com.darkona.dropletsofthirst.compat.create.SandFilterBlockEntity;
import com.darkona.dropletsofthirst.api.PurityLevel;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
import com.darkona.dropletsofthirst.foundation.config.PurityConfig;
import com.darkona.dropletsofthirst.foundation.tab.ThirstTab;
import com.darkona.dropletsofthirst.content.registry.ItemInit;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Recipe;
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

import java.util.List;

/**
 * Create: open pipe ends and the Sand Filter. Registered by {@link DropletsGameTests} only when Create is installed.
 */
@PrefixGameTestTemplate(false)
public class CreateTests
{
    @GameTest(template = "box", templateNamespace = DropletsOfThirst.ID)
    public static void openPipeGetsCauldronPurity(GameTestHelper helper)
    {
        BlockPos cauldron = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(cauldron.below(), Blocks.CAMPFIRE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        FluidStack drained = drainThroughPipe(helper, cauldron.above());
        helper.assertFalse(drained.isEmpty(), "the pipe drained nothing from the cauldron");
        helper.assertValueEqual(WaterPurity.getPurity(drained), WaterPurity.HEATED_CAULDRON_PURITY, "purity of water drained from a cauldron on a campfire");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = DropletsOfThirst.ID)
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

    @GameTest(template = "box", templateNamespace = DropletsOfThirst.ID)
    public static void openPipePouredWaterKeepsItsPurity(GameTestHelper helper)
    {
        BlockPos water = helper.absolutePos(new BlockPos(2, 1, 2));
        int contaminated = PurityLevel.CONTAMINATED.level();
        helper.assertTrue(WaterPurity.getWaterPurity(helper.getLevel(), helper.getLevel().getBiome(water), water, true, null) > contaminated, "world water at the test position is already contaminated");
        OpenEndedPipe pipe = new OpenEndedPipe(new BlockFace(water.above(), Direction.DOWN));
        pipe.manageSource(helper.getLevel(), null);
        IFluidHandler handler = pipe.provideHandler().getCapability();
        // The pipe pours once its internal tank is full, like a pump filling it tick after tick.
        for (int i = 0; i < 10 && !helper.getLevel().getFluidState(water).isSource(); i++)
            handler.fill(water(contaminated, 250), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(helper.getLevel().getFluidState(water).isSource(), "the open pipe end placed no water source");
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), water), contaminated, "purity of water poured by an open pipe end");
        FluidStack drained = drainThroughPipe(helper, water.above());
        helper.assertValueEqual(WaterPurity.getPurity(drained), contaminated, "purity of poured water drained back by a pipe");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = DropletsOfThirst.ID)
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

    @GameTest(template = "box", templateNamespace = DropletsOfThirst.ID, timeoutTicks = 400)
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

    /**
     * A filter hands its water on to a filter placed in front of it after it was filtered, and, once turned, to the one
     * in its new front; not to a filter that was taken away.
     */
    @GameTest(template = "box", templateNamespace = DropletsOfThirst.ID, timeoutTicks = 400)
    public static void sandFilterFindsTheFilterInFrontWhenItChanges(GameTestHelper helper)
    {
        BlockPos first = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos below = first.below();
        BlockPos east = first.east();
        BlockState filter = CreateRegistry.SAND_FILTER_BLOCK.get().defaultBlockState();
        helper.getLevel().setBlockAndUpdate(first, filter);
        helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, first, Direction.UP).fill(water(0, 200), IFluidHandler.FluidAction.EXECUTE);
        int twice = SandFilterBlockEntity.filteredPurity(SandFilterBlockEntity.filteredPurity(0));
        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    helper.assertValueEqual(purified(helper, first, Direction.DOWN).getAmount(), 200, "water waiting in a filter with nothing in front");
                    helper.getLevel().setBlockAndUpdate(below, filter);
                })
                .thenWaitUntil(() -> {
                    FluidStack out = purified(helper, below, Direction.DOWN);
                    helper.assertValueEqual(out.getAmount(), 200, "water out of the filter placed in front later");
                    helper.assertValueEqual(WaterPurity.getPurity(out), twice, "purity after the filter placed later");
                })
                .thenExecute(() -> {
                    helper.getLevel().setBlockAndUpdate(below, Blocks.AIR.defaultBlockState());
                    helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, first, Direction.UP).fill(water(0, 200), IFluidHandler.FluidAction.EXECUTE);
                })
                .thenExecuteAfter(40, () -> {
                    helper.assertValueEqual(purified(helper, first, Direction.DOWN).getAmount(), 200, "water handed on to a filter that was taken away");
                    helper.getLevel().setBlockAndUpdate(east, filter.setValue(SandFilterBlock.FACING, Direction.EAST));
                    helper.getLevel().setBlockAndUpdate(first, filter.setValue(SandFilterBlock.FACING, Direction.EAST));
                })
                .thenWaitUntil(() -> {
                    FluidStack out = purified(helper, east, Direction.EAST);
                    helper.assertValueEqual(out.getAmount(), 200, "water out of the filter in front after turning");
                    helper.assertValueEqual(WaterPurity.getPurity(out), twice, "purity after turning");
                })
                .thenSucceed();
    }

    private static FluidStack purified(GameTestHelper helper, BlockPos pos, Direction facing)
    {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, facing).getFluidInTank(0);
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void sandFilterStopsAtMaxPurity(GameTestHelper helper)
    {
        int max = CompatConfig.SAND_FILTER_MAX_PURITY.get();
        try
        {
            CompatConfig.SAND_FILTER_MAX_PURITY.set(1);
            helper.assertValueEqual(SandFilterBlockEntity.filteredPurity(0), 1, "contaminated water with sandFilterMaxPurity 1");
            helper.assertValueEqual(SandFilterBlockEntity.filteredPurity(1), 1, "dirty water with sandFilterMaxPurity 1");
            helper.assertValueEqual(SandFilterBlockEntity.filteredPurity(3), 3, "acceptable water is never made dirtier");
            CompatConfig.SAND_FILTER_MAX_PURITY.set(max);
            helper.assertValueEqual(max, PurityLevel.PURE.level(), "default sandFilterMaxPurity");
            helper.assertValueEqual(SandFilterBlockEntity.filteredPurity(PurityLevel.CLEAN.level()), PurityLevel.PURE.level(), "clean water through a filter");
        }
        finally
        {
            CompatConfig.SAND_FILTER_MAX_PURITY.set(max);
        }
        helper.succeed();
    }

    /** Water below the rate per tick (the last millibuckets a pipe delivers) is filtered too, not left in the dirty tank. */
    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void sandFilterFiltersLessThanItsRate(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(pos, CreateRegistry.SAND_FILTER_BLOCK.get().defaultBlockState());
        SandFilterBlockEntity filter = (SandFilterBlockEntity) helper.getLevel().getBlockEntity(pos);
        int rate = CompatConfig.SAND_FILTER_MB_PER_TICK.get();
        int amount = rate + rate / 2;
        helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP).fill(water(0, amount), IFluidHandler.FluidAction.EXECUTE);
        for (int tick = 0; tick < 10; tick++)
            filter.tick();
        FluidStack out = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.DOWN).getFluidInTank(0);
        helper.assertValueEqual(out.getAmount(), amount, "water out of a filter fed less than its rate");
        helper.assertValueEqual(WaterPurity.getPurity(out), SandFilterBlockEntity.filteredPurity(0), "purity of the water out");
        helper.assertValueEqual(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP).getFluidInTank(0).getAmount(), 0, "water left in the dirty tank");
        helper.succeed();
    }

    /** Asked without a side (Jade, other inspectors), the filter shows both tanks and lets nothing in or out. */
    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void sandFilterShowsItsTanksWithoutASide(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(pos, CreateRegistry.SAND_FILTER_BLOCK.get().defaultBlockState());
        helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP).fill(water(0, 500), IFluidHandler.FluidAction.EXECUTE);
        IFluidHandler tanks = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        helper.assertTrue(tanks != null, "no fluid handler without a side");
        helper.assertValueEqual(tanks.getTanks(), 2, "tanks seen without a side");
        helper.assertValueEqual(tanks.getFluidInTank(0).getAmount(), 500, "dirty water seen without a side");
        helper.assertTrue(tanks.getFluidInTank(1).isEmpty(), "the purified tank should be empty");
        helper.assertValueEqual(tanks.fill(water(0, 100), IFluidHandler.FluidAction.EXECUTE), 0, "water filled without a side");
        helper.assertTrue(tanks.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "water drained without a side");
        helper.assertValueEqual(tanks.getFluidInTank(0).getAmount(), 500, "dirty water after a fill and a drain without a side");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
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

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void sandFilterPassesWaterUnchangedWithPurityOff(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(pos, CreateRegistry.SAND_FILTER_BLOCK.get().defaultBlockState());
        SandFilterBlockEntity filter = (SandFilterBlockEntity) helper.getLevel().getBlockEntity(pos);
        boolean enabled = PurityConfig.ENABLED.get();
        try
        {
            PurityConfig.ENABLED.set(false);
            helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            for (int tick = 0; tick < 1000 && filter.hasFluid() && helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.DOWN).getFluidInTank(0).getAmount() < 1000; tick++)
                filter.tick();
            FluidStack out = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.DOWN).getFluidInTank(0);
            helper.assertValueEqual(out.getAmount(), 1000, "water through a filter with purity off");
            helper.assertFalse(WaterPurity.hasPurity(out), "water through a filter has a purity with purity off");
        }
        finally
        {
            PurityConfig.ENABLED.set(enabled);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void cactusRecipeFollowsPurityToggle(GameTestHelper helper)
    {
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(DropletsOfThirst.asResource("compat/create/cactus")).isPresent(), "cactus compacting with purity is missing with purity on");
        helper.assertFalse(recipes.byKey(DropletsOfThirst.asResource("compat/create/cactus_without_purity")).isPresent(), "cactus compacting without purity is loaded with purity on");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void fanWashingPurifiesUpToClean(GameTestHelper helper)
    {
        int clean = PurityLevel.CLEAN.level();
        for (ItemStack container : List.of(PotionContents.createItemStack(Items.POTION, Potions.WATER), new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get())))
        {
            for (int purity = PurityLevel.MIN; purity < clean; purity++)
                helper.assertValueEqual(fanned(helper, AllFanProcessingTypes.SPLASHING, container, purity), purity + 1, "purity " + purity + " " + container + " washed by a fan");
            for (int purity = clean; purity <= PurityLevel.MAX; purity++)
                helper.assertFalse(AllFanProcessingTypes.SPLASHING.canProcess(WaterPurity.addPurity(container.copy(), purity), helper.getLevel()), "a fan washes " + container + " of purity " + purity);
        }
        // Create hands back a bucket's crafting remainder next to the result: washing water buckets would make buckets.
        helper.assertFalse(AllFanProcessingTypes.SPLASHING.canProcess(WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), 0), helper.getLevel()), "a fan washes water buckets");
        helper.succeed();
    }

    /**
     * Fans smoke with the smoker recipes (the {@code purify_smoking} pack, on in the gametest world) and blast with the
     * furnace ones; with both packs on, Create burns what can be smoked in a blasting fan, as it does with food.
     */
    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void smokingFanPurifiesWithTheSmokerRecipes(GameTestHelper helper)
    {
        ItemStack bottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        // Smoker recipes add two levels, and stop at clean with Create.
        int clean = PurityLevel.CLEAN.level();
        for (int purity = PurityLevel.MIN; purity < clean; purity++)
            helper.assertValueEqual(fanned(helper, AllFanProcessingTypes.SMOKING, bottle, purity), Math.min(purity + 2, clean), "water bottle of purity " + purity + " smoked by a fan");
        helper.succeed();
    }

    /**
     * Purity of what a fan of {@code type} turns {@code container} of {@code purity} into; -1 when it gives nothing.
     */
    private static int fanned(GameTestHelper helper, FanProcessingType type, ItemStack container, int purity)
    {
        ItemStack input = WaterPurity.addPurity(container.copy(), purity);
        helper.assertTrue(type.canProcess(input, helper.getLevel()), "a fan does not process " + input);
        List<ItemStack> results = type.process(input, helper.getLevel());
        if (results == null || results.size() != 1)
            return -1;
        helper.assertTrue(ItemStack.isSameItem(results.get(0), container), "a fan turned " + input + " into " + results.get(0));
        return WaterPurity.getPurity(results.get(0));
    }

    @GameTest(template = "box", templateNamespace = DropletsOfThirst.ID)
    public static void heatedBasinPurifiesUpToClean(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED));
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.BASIN.getDefaultState());
        BasinBlockEntity basin = (BasinBlockEntity) helper.getLevel().getBlockEntity(pos);
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(DropletsOfThirst.asResource("compat/create/water_from_heated_mixing_to_dirty")).orElseThrow().value();
        helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null).fill(water(0, 250), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(BasinRecipe.match(basin, recipe), "dirty water does not match the heated mixing recipe");
        helper.assertTrue(BasinRecipe.apply(basin, recipe), "the heated mixing recipe did not run");
        FluidStack out = basin.getTanks().getSecond().getPrimaryHandler().getFluidInTank(0);
        helper.assertValueEqual(out.getAmount(), 250, "water out of the heated basin");
        helper.assertValueEqual(WaterPurity.getPurity(out), PurityLevel.DIRTY.level(), "purity out of the heated basin, contaminated water in");

        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.BASIN.getDefaultState());
        BasinBlockEntity cold = (BasinBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null).fill(water(0, 250), IFluidHandler.FluidAction.EXECUTE);
        helper.assertFalse(BasinRecipe.match(cold, recipe), "an unheated basin purifies water");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void spoutFillingKeepsPurity(GameTestHelper helper)
    {
        ItemStack bowl = FillingBySpout.fillItem(helper.getLevel(), 250, new ItemStack(ItemInit.TERRACOTTA_BOWL.get()), water(0, 1000));
        helper.assertTrue(bowl.is(ItemInit.TERRACOTTA_WATER_BOWL.get()), "a spout filled a terracotta bowl into " + bowl);
        helper.assertValueEqual(WaterPurity.getPurity(bowl), 0, "purity of a terracotta bowl filled by a spout with contaminated water");
        ItemStack bottle = FillingBySpout.fillItem(helper.getLevel(), 250, new ItemStack(Items.GLASS_BOTTLE), water(1, 1000));
        helper.assertValueEqual(WaterPurity.getPurity(bottle), 1, "purity of a bottle filled by a spout with dirty water");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void emptyingKeepsPurity(GameTestHelper helper)
    {
        ItemStack bottle = WaterPurity.addPurity(PotionContents.createItemStack(Items.POTION, Potions.WATER), 0);
        FluidStack fromBottle = GenericItemEmptying.emptyItem(helper.getLevel(), bottle, false).getFirst();
        helper.assertTrue(bottle.isEmpty(), "the last bottle was not used up");
        helper.assertValueEqual(WaterPurity.getPurity(fromBottle), 0, "purity of water emptied from the last contaminated bottle");

        ItemStack bowl = WaterPurity.addPurity(new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()), 1);
        Pair<FluidStack, ItemStack> fromBowl = GenericItemEmptying.emptyItem(helper.getLevel(), bowl, true);
        helper.assertTrue(fromBowl.getSecond().is(ItemInit.TERRACOTTA_BOWL.get()), "emptying a terracotta water bowl gave " + fromBowl.getSecond());
        helper.assertValueEqual(fromBowl.getFirst().getAmount(), 250, "water emptied from a terracotta bowl");
        helper.assertValueEqual(WaterPurity.getPurity(fromBowl.getFirst()), 1, "purity of water emptied from a dirty terracotta bowl");
        FluidStack plain = GenericItemEmptying.emptyItem(helper.getLevel(), new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()), true).getFirst();
        helper.assertFalse(WaterPurity.hasPurity(plain), "the emptying recipe kept the purity of an earlier bowl");
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
