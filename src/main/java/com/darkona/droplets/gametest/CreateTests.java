package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.create.CreateRegistry;
import com.darkona.droplets.compat.create.SandFilterBlock;
import com.darkona.droplets.compat.create.SandFilterBlockEntity;
import com.darkona.droplets.api.PurityLevel;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.CompatConfig;
import com.darkona.droplets.foundation.config.PurityConfig;
import com.darkona.droplets.foundation.tab.ThirstTab;
import com.darkona.droplets.content.registry.ItemInit;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.utility.Pair;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Recipe;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import com.simibubi.create.foundation.utility.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.darkona.droplets.gametest.TestSupport.assertValueEqual;
import static com.darkona.droplets.gametest.TestSupport.assertTrue;
import static com.darkona.droplets.gametest.TestSupport.assertFalse;

/**
 * Create: open pipe ends and the Sand Filter. Registered by {@link DropletsGameTests} only when Create is installed.
 */
@PrefixGameTestTemplate(false)
public class CreateTests
{
    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void openPipeGetsCauldronPurity(GameTestHelper helper)
    {
        BlockPos cauldron = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(cauldron.below(), Blocks.CAMPFIRE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        FluidStack drained = drainThroughPipe(helper, cauldron.above());
        assertFalse(helper, drained.isEmpty(), "the pipe drained nothing from the cauldron");
        assertValueEqual(helper, WaterPurity.getPurity(drained), WaterPurity.HEATED_CAULDRON_PURITY, "purity of water drained from a cauldron on a campfire");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void openPipeKeepsWorldPurity(GameTestHelper helper)
    {
        BlockPos water = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        int expected = WaterPurity.getWaterPurity(helper.getLevel(), water, true);
        FluidStack drained = drainThroughPipe(helper, water.above());
        assertFalse(helper, drained.isEmpty(), "the pipe drained nothing from the water source");
        assertTrue(helper, WaterPurity.hasPurity(drained), "water drained from the world has no purity");
        assertValueEqual(helper, WaterPurity.getPurity(drained), expected, "purity of water drained from the world");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void openPipePouredWaterKeepsItsPurity(GameTestHelper helper)
    {
        // The game test world of 1.19.2 can put the test in an ocean, whose water is always contaminated.
        int salt = PurityConfig.SALT_WATER_PURITY.get();
        try
        {
            TestSupport.set(PurityConfig.SALT_WATER_PURITY, -1);
            openPipePouredWater(helper);
        }
        finally
        {
            TestSupport.set(PurityConfig.SALT_WATER_PURITY, salt);
        }
    }

    private static void openPipePouredWater(GameTestHelper helper)
    {
        BlockPos water = helper.absolutePos(new BlockPos(2, 1, 2));
        int contaminated = PurityLevel.CONTAMINATED.level();
        assertTrue(helper, WaterPurity.getWaterPurity(helper.getLevel(), helper.getLevel().getBiome(water), water, true, null) > contaminated, "world water at the test position is already contaminated");
        OpenEndedPipe pipe = new OpenEndedPipe(new BlockFace(water.above(), Direction.DOWN));
        pipe.manageSource(helper.getLevel());
        IFluidHandler handler = pipe.provideHandler().orElseThrow(IllegalStateException::new);
        // The pipe pours once its internal tank is full, like a pump filling it tick after tick.
        for (int i = 0; i < 10 && !helper.getLevel().getFluidState(water).isSource(); i++)
            handler.fill(water(contaminated, 250), IFluidHandler.FluidAction.EXECUTE);
        assertTrue(helper, helper.getLevel().getFluidState(water).isSource(), "the open pipe end placed no water source");
        assertValueEqual(helper, WaterPurity.getBlockPurity(helper.getLevel(), water), contaminated, "purity of water poured by an open pipe end");
        FluidStack drained = drainThroughPipe(helper, water.above());
        assertValueEqual(helper, WaterPurity.getPurity(drained), contaminated, "purity of poured water drained back by a pipe");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void sandFilterTurnsWithItsFacing(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, CreateRegistry.SAND_FILTER_BLOCK.get().defaultBlockState().setValue(SandFilterBlock.FACING, Direction.EAST));
        IFluidHandler out = TestSupport.fluidHandler(helper, pos, Direction.EAST);
        IFluidHandler in = TestSupport.fluidHandler(helper, pos, Direction.WEST);
        assertTrue(helper, out != null && in != null && out != in, "an east-facing filter has its tanks east and west");
        assertTrue(helper, TestSupport.fluidHandler(helper, pos, Direction.UP) == null, "an east-facing filter has no tank on top");
        assertValueEqual(helper, in.fill(water(0, 1000), IFluidHandler.FluidAction.EXECUTE), 1000, "water accepted on the input side");
        assertValueEqual(helper, out.fill(water(0, 1000), IFluidHandler.FluidAction.SIMULATE), 0, "water accepted on the output side");
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
        IFluidHandler in = TestSupport.fluidHandler(helper, top, Direction.UP);
        in.fill(water(0, 1000), IFluidHandler.FluidAction.EXECUTE);
        int expected = SandFilterBlockEntity.filteredPurity(SandFilterBlockEntity.filteredPurity(0));
        helper.succeedWhen(() -> {
            FluidStack out = TestSupport.fluidHandler(helper, bottom, Direction.DOWN).getFluidInTank(0);
            assertValueEqual(helper, out.getAmount(), 1000, "water out of the second filter");
            assertValueEqual(helper, WaterPurity.getPurity(out), expected, "purity after two filters");
        });
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void sandFilterStopsAtMaxPurity(GameTestHelper helper)
    {
        int max = CompatConfig.SAND_FILTER_MAX_PURITY.get();
        try
        {
            TestSupport.set(CompatConfig.SAND_FILTER_MAX_PURITY, 1);
            assertValueEqual(helper, SandFilterBlockEntity.filteredPurity(0), 1, "contaminated water with sandFilterMaxPurity 1");
            assertValueEqual(helper, SandFilterBlockEntity.filteredPurity(1), 1, "dirty water with sandFilterMaxPurity 1");
            assertValueEqual(helper, SandFilterBlockEntity.filteredPurity(3), 3, "acceptable water is never made dirtier");
            TestSupport.set(CompatConfig.SAND_FILTER_MAX_PURITY, max);
            assertValueEqual(helper, max, PurityLevel.PURE.level(), "default sandFilterMaxPurity");
            assertValueEqual(helper, SandFilterBlockEntity.filteredPurity(PurityLevel.CLEAN.level()), PurityLevel.PURE.level(), "clean water through a filter");
        }
        finally
        {
            TestSupport.set(CompatConfig.SAND_FILTER_MAX_PURITY, max);
        }
        helper.succeed();
    }

    /** Water below the rate per tick (the last millibuckets a pipe delivers) is filtered too, not left in the dirty tank. */
    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void sandFilterFiltersLessThanItsRate(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(pos, CreateRegistry.SAND_FILTER_BLOCK.get().defaultBlockState());
        SandFilterBlockEntity filter = (SandFilterBlockEntity) helper.getLevel().getBlockEntity(pos);
        int rate = CompatConfig.SAND_FILTER_MB_PER_TICK.get();
        int amount = rate + rate / 2;
        tank(helper, pos, Direction.UP).fill(water(0, amount), IFluidHandler.FluidAction.EXECUTE);
        for (int tick = 0; tick < 10; tick++)
            filter.tick();
        FluidStack out = tank(helper, pos, Direction.DOWN).getFluidInTank(0);
        assertValueEqual(helper, out.getAmount(), amount, "water out of a filter fed less than its rate");
        assertValueEqual(helper, WaterPurity.getPurity(out), SandFilterBlockEntity.filteredPurity(0), "purity of the water out");
        assertValueEqual(helper, tank(helper, pos, Direction.UP).getFluidInTank(0).getAmount(), 0, "water left in the dirty tank");
        helper.succeed();
    }

    /** Asked without a side (Jade, other inspectors), the filter shows both tanks and lets nothing in or out. */
    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void sandFilterShowsItsTanksWithoutASide(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(pos, CreateRegistry.SAND_FILTER_BLOCK.get().defaultBlockState());
        tank(helper, pos, Direction.UP).fill(water(0, 500), IFluidHandler.FluidAction.EXECUTE);
        IFluidHandler tanks = helper.getLevel().getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER, null).resolve().orElse(null);
        assertTrue(helper, tanks != null, "no fluid handler without a side");
        assertValueEqual(helper, tanks.getTanks(), 2, "tanks seen without a side");
        assertValueEqual(helper, tanks.getFluidInTank(0).getAmount(), 500, "dirty water seen without a side");
        assertTrue(helper, tanks.getFluidInTank(1).isEmpty(), "the purified tank should be empty");
        assertValueEqual(helper, tanks.fill(water(0, 100), IFluidHandler.FluidAction.EXECUTE), 0, "water filled without a side");
        assertTrue(helper, tanks.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "water drained without a side");
        assertValueEqual(helper, tanks.getFluidInTank(0).getAmount(), 500, "dirty water after a fill and a drain without a side");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void sandFilterIsInOwnCreativeTab(GameTestHelper helper)
    {
        assertTrue(helper, ThirstTab.DisplayItems().stream().anyMatch(stack -> stack.is(CreateRegistry.SAND_FILTER_BLOCK.get().asItem())), "Sand Filter missing from the Blue Droplets tab");
        assertTrue(helper, CreateRegistry.SAND_FILTER_BLOCK.get().asItem().getItemCategory() == ThirstTab.THIRST_TAB, "the Sand Filter item is not in the Blue Droplets tab");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void sandFilterPassesWaterUnchangedWithPurityOff(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        helper.getLevel().setBlockAndUpdate(pos, CreateRegistry.SAND_FILTER_BLOCK.get().defaultBlockState());
        SandFilterBlockEntity filter = (SandFilterBlockEntity) helper.getLevel().getBlockEntity(pos);
        boolean enabled = PurityConfig.ENABLED.get();
        try
        {
            TestSupport.set(PurityConfig.ENABLED, false);
            TestSupport.fluidHandler(helper, pos, Direction.UP).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            for (int tick = 0; tick < 1000 && filter.hasFluid() && TestSupport.fluidHandler(helper, pos, Direction.DOWN).getFluidInTank(0).getAmount() < 1000; tick++)
                filter.tick();
            FluidStack out = TestSupport.fluidHandler(helper, pos, Direction.DOWN).getFluidInTank(0);
            assertValueEqual(helper, out.getAmount(), 1000, "water through a filter with purity off");
            assertFalse(helper, WaterPurity.hasPurity(out), "water through a filter has a purity with purity off");
        }
        finally
        {
            TestSupport.set(PurityConfig.ENABLED, enabled);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void cactusRecipeFollowsPurityToggle(GameTestHelper helper)
    {
        var recipes = helper.getLevel().getRecipeManager();
        assertTrue(helper, recipes.byKey(BlueDroplets.asResource("compat/create/cactus")).isPresent(), "cactus compacting with purity is missing with purity on");
        assertFalse(helper, recipes.byKey(BlueDroplets.asResource("compat/create/cactus_without_purity")).isPresent(), "cactus compacting without purity is loaded with purity on");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void fanWashingPurifiesUpToClean(GameTestHelper helper)
    {
        int clean = PurityLevel.CLEAN.level();
        for (ItemStack container : List.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get())))
        {
            for (int purity = PurityLevel.MIN; purity < clean; purity++)
                assertValueEqual(helper, fanned(helper, AllFanProcessingTypes.SPLASHING, container, purity), purity + 1, "purity " + purity + " " + container + " washed by a fan");
            for (int purity = clean; purity <= PurityLevel.MAX; purity++)
                assertFalse(helper, AllFanProcessingTypes.SPLASHING.canProcess(WaterPurity.addPurity(container.copy(), purity), helper.getLevel()), "a fan washes " + container + " of purity " + purity);
        }
        // Create hands back a bucket's crafting remainder next to the result: washing water buckets would make buckets.
        assertFalse(helper, AllFanProcessingTypes.SPLASHING.canProcess(WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), 0), helper.getLevel()), "a fan washes water buckets");
        helper.succeed();
    }

    /**
     * Fans smoke with the smoker recipes (the {@code purify_smoking} pack, on in the gametest world) and blast with the
     * furnace ones; with both packs on, Create burns what can be smoked in a blasting fan, as it does with food.
     */
    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void smokingFanPurifiesWithTheSmokerRecipes(GameTestHelper helper)
    {
        ItemStack bottle = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
        // The smoker pack is opt-in and stays off in the game test world: without it, a smoking fan leaves water alone.
        if (!helper.getLevel().getServer().getPackRepository().getSelectedIds().contains("mod/" + BlueDroplets.ID + ":datapacks/purify_smoking"))
        {
            assertFalse(helper, AllFanProcessingTypes.SMOKING.canProcess(WaterPurity.addPurity(bottle.copy(), 0), helper.getLevel()), "a smoking fan purifies water without the smoker pack");
            helper.succeed();
            return;
        }
        // Smoker recipes add two levels, and stop at clean with Create.
        int clean = PurityLevel.CLEAN.level();
        for (int purity = PurityLevel.MIN; purity < clean; purity++)
            assertValueEqual(helper, fanned(helper, AllFanProcessingTypes.SMOKING, bottle, purity), Math.min(purity + 2, clean), "water bottle of purity " + purity + " smoked by a fan");
        helper.succeed();
    }

    /**
     * Purity of what a fan of {@code type} turns {@code container} of {@code purity} into; -1 when it gives nothing.
     */
    private static int fanned(GameTestHelper helper, FanProcessingType type, ItemStack container, int purity)
    {
        ItemStack input = WaterPurity.addPurity(container.copy(), purity);
        assertTrue(helper, type.canProcess(input, helper.getLevel()), "a fan does not process " + input);
        List<ItemStack> results = type.process(input, helper.getLevel());
        if (results == null || results.size() != 1)
            return -1;
        assertTrue(helper, ItemStack.isSame(results.get(0), container), "a fan turned " + input + " into " + results.get(0));
        return WaterPurity.getPurity(results.get(0));
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void heatedBasinPurifiesUpToClean(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED));
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.BASIN.getDefaultState());
        BasinBlockEntity basin = (BasinBlockEntity) helper.getLevel().getBlockEntity(pos);
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(BlueDroplets.asResource("compat/create/water_from_heated_mixing_to_dirty")).orElseThrow();
        TestSupport.fluidHandler(helper, pos, null).fill(water(0, 250), IFluidHandler.FluidAction.EXECUTE);
        assertTrue(helper, BasinRecipe.match(basin, recipe), "dirty water does not match the heated mixing recipe");
        assertTrue(helper, BasinRecipe.apply(basin, recipe), "the heated mixing recipe did not run");
        FluidStack out = basin.getTanks().getSecond().getPrimaryHandler().getFluidInTank(0);
        assertValueEqual(helper, out.getAmount(), 250, "water out of the heated basin");
        assertValueEqual(helper, WaterPurity.getPurity(out), PurityLevel.DIRTY.level(), "purity out of the heated basin, contaminated water in");

        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.BASIN.getDefaultState());
        BasinBlockEntity cold = (BasinBlockEntity) helper.getLevel().getBlockEntity(pos);
        TestSupport.fluidHandler(helper, pos, null).fill(water(0, 250), IFluidHandler.FluidAction.EXECUTE);
        assertFalse(helper, BasinRecipe.match(cold, recipe), "an unheated basin purifies water");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void spoutFillingKeepsPurity(GameTestHelper helper)
    {
        ItemStack bowl = FillingBySpout.fillItem(helper.getLevel(), 250, new ItemStack(ItemInit.TERRACOTTA_BOWL.get()), water(0, 1000));
        assertTrue(helper, bowl.is(ItemInit.TERRACOTTA_WATER_BOWL.get()), "a spout filled a terracotta bowl into " + bowl);
        assertValueEqual(helper, WaterPurity.getPurity(bowl), 0, "purity of a terracotta bowl filled by a spout with contaminated water");
        ItemStack bottle = FillingBySpout.fillItem(helper.getLevel(), 250, new ItemStack(Items.GLASS_BOTTLE), water(1, 1000));
        assertValueEqual(helper, WaterPurity.getPurity(bottle), 1, "purity of a bottle filled by a spout with dirty water");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void emptyingKeepsPurity(GameTestHelper helper)
    {
        ItemStack bottle = WaterPurity.addPurity(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), 0);
        FluidStack fromBottle = GenericItemEmptying.emptyItem(helper.getLevel(), bottle, false).getFirst();
        assertTrue(helper, bottle.isEmpty(), "the last bottle was not used up");
        assertValueEqual(helper, WaterPurity.getPurity(fromBottle), 0, "purity of water emptied from the last contaminated bottle");

        ItemStack bowl = WaterPurity.addPurity(new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()), 1);
        Pair<FluidStack, ItemStack> fromBowl = GenericItemEmptying.emptyItem(helper.getLevel(), bowl, true);
        assertTrue(helper, fromBowl.getSecond().is(ItemInit.TERRACOTTA_BOWL.get()), "emptying a terracotta water bowl gave " + fromBowl.getSecond());
        assertValueEqual(helper, fromBowl.getFirst().getAmount(), 250, "water emptied from a terracotta bowl");
        assertValueEqual(helper, WaterPurity.getPurity(fromBowl.getFirst()), 1, "purity of water emptied from a dirty terracotta bowl");
        FluidStack plain = GenericItemEmptying.emptyItem(helper.getLevel(), new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()), true).getFirst();
        assertFalse(helper, WaterPurity.hasPurity(plain), "the emptying recipe kept the purity of an earlier bowl");
        helper.succeed();
    }

    private static IFluidHandler tank(GameTestHelper helper, BlockPos pos, Direction side)
    {
        return helper.getLevel().getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER, side).orElseThrow(IllegalStateException::new);
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
        pipe.manageSource(helper.getLevel());
        IFluidHandler handler = pipe.provideHandler().orElseThrow(IllegalStateException::new);
        return handler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
    }
}
