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
import net.createmod.catnip.data.Pair;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.darkona.droplets.gametest.TestSupport.assertValueEqual;

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
        helper.assertFalse(drained.isEmpty(), "the pipe drained nothing from the cauldron");
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
        helper.assertFalse(drained.isEmpty(), "the pipe drained nothing from the water source");
        helper.assertTrue(WaterPurity.hasPurity(drained), "water drained from the world has no purity");
        assertValueEqual(helper, WaterPurity.getPurity(drained), expected, "purity of water drained from the world");
        helper.succeed();
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
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
        IFluidHandler out = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.EAST);
        IFluidHandler in = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.WEST);
        helper.assertTrue(out != null && in != null && out != in, "an east-facing filter has its tanks east and west");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP) == null, "an east-facing filter has no tank on top");
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
        IFluidHandler in = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, top, Direction.UP);
        in.fill(water(0, 1000), IFluidHandler.FluidAction.EXECUTE);
        int expected = SandFilterBlockEntity.filteredPurity(SandFilterBlockEntity.filteredPurity(0));
        helper.succeedWhen(() -> {
            FluidStack out = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, bottom, Direction.DOWN).getFluidInTank(0);
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
            helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP).fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
            for (int tick = 0; tick < 1000 && filter.hasFluid() && helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.DOWN).getFluidInTank(0).getAmount() < 1000; tick++)
                filter.tick();
            FluidStack out = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.DOWN).getFluidInTank(0);
            assertValueEqual(helper, out.getAmount(), 1000, "water through a filter with purity off");
            helper.assertFalse(WaterPurity.hasPurity(out), "water through a filter has a purity with purity off");
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
        helper.assertTrue(recipes.byKey(BlueDroplets.asResource("compat/create/cactus")).isPresent(), "cactus compacting with purity is missing with purity on");
        helper.assertFalse(recipes.byKey(BlueDroplets.asResource("compat/create/cactus_without_purity")).isPresent(), "cactus compacting without purity is loaded with purity on");
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
    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void smokingFanPurifiesWithTheSmokerRecipes(GameTestHelper helper)
    {
        ItemStack bottle = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
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
        helper.assertTrue(type.canProcess(input, helper.getLevel()), "a fan does not process " + input);
        List<ItemStack> results = type.process(input, helper.getLevel());
        if (results == null || results.size() != 1)
            return -1;
        helper.assertTrue(ItemStack.isSameItem(results.get(0), container), "a fan turned " + input + " into " + results.get(0));
        return WaterPurity.getPurity(results.get(0));
    }

    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
    public static void heatedBasinPurifiesUpToClean(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED));
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.BASIN.getDefaultState());
        BasinBlockEntity basin = (BasinBlockEntity) helper.getLevel().getBlockEntity(pos);
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(BlueDroplets.asResource("compat/create/water_from_heated_mixing_to_dirty")).orElseThrow().value();
        helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null).fill(water(0, 250), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(BasinRecipe.match(basin, recipe), "dirty water does not match the heated mixing recipe");
        helper.assertTrue(BasinRecipe.apply(basin, recipe), "the heated mixing recipe did not run");
        FluidStack out = basin.getTanks().getSecond().getPrimaryHandler().getFluidInTank(0);
        assertValueEqual(helper, out.getAmount(), 250, "water out of the heated basin");
        assertValueEqual(helper, WaterPurity.getPurity(out), PurityLevel.DIRTY.level(), "purity out of the heated basin, contaminated water in");

        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.BASIN.getDefaultState());
        BasinBlockEntity cold = (BasinBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null).fill(water(0, 250), IFluidHandler.FluidAction.EXECUTE);
        helper.assertFalse(BasinRecipe.match(cold, recipe), "an unheated basin purifies water");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void spoutFillingKeepsPurity(GameTestHelper helper)
    {
        ItemStack bowl = FillingBySpout.fillItem(helper.getLevel(), 250, new ItemStack(ItemInit.TERRACOTTA_BOWL.get()), water(0, 1000));
        helper.assertTrue(bowl.is(ItemInit.TERRACOTTA_WATER_BOWL.get()), "a spout filled a terracotta bowl into " + bowl);
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
        helper.assertTrue(bottle.isEmpty(), "the last bottle was not used up");
        assertValueEqual(helper, WaterPurity.getPurity(fromBottle), 0, "purity of water emptied from the last contaminated bottle");

        ItemStack bowl = WaterPurity.addPurity(new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()), 1);
        Pair<FluidStack, ItemStack> fromBowl = GenericItemEmptying.emptyItem(helper.getLevel(), bowl, true);
        helper.assertTrue(fromBowl.getSecond().is(ItemInit.TERRACOTTA_BOWL.get()), "emptying a terracotta water bowl gave " + fromBowl.getSecond());
        assertValueEqual(helper, fromBowl.getFirst().getAmount(), 250, "water emptied from a terracotta bowl");
        assertValueEqual(helper, WaterPurity.getPurity(fromBowl.getFirst()), 1, "purity of water emptied from a dirty terracotta bowl");
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
