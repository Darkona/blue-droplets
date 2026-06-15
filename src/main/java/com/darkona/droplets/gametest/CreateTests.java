package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.create.CreateRegistry;
import com.darkona.droplets.compat.create.SandFilterBlock;
import com.darkona.droplets.compat.create.SandFilterBlockEntity;
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
    @GameTest(template = "box", templateNamespace = BlueDroplets.ID)
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

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
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

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void cactusRecipeFollowsPurityToggle(GameTestHelper helper)
    {
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(BlueDroplets.asResource("compat/create/cactus")).isPresent(), "cactus compacting with purity is missing with purity on");
        helper.assertFalse(recipes.byKey(BlueDroplets.asResource("compat/create/cactus_without_purity")).isPresent(), "cactus compacting without purity is loaded with purity on");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void fanWashingPurifiesUpToAcceptable(GameTestHelper helper)
    {
        for (ItemStack container : List.of(PotionContents.createItemStack(Items.POTION, Potions.WATER), new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get())))
        {
            helper.assertValueEqual(fanned(helper, AllFanProcessingTypes.SPLASHING, container, 0), 1, "dirty " + container + " washed by a fan");
            helper.assertValueEqual(fanned(helper, AllFanProcessingTypes.SPLASHING, container, 1), 2, "slightly dirty " + container + " washed by a fan");
            helper.assertFalse(AllFanProcessingTypes.SPLASHING.canProcess(WaterPurity.addPurity(container.copy(), 2), helper.getLevel()), "a fan washes acceptable " + container);
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
        ItemStack bottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        helper.assertValueEqual(fanned(helper, AllFanProcessingTypes.SMOKING, bottle, 0), 2, "dirty water bottle smoked by a fan");
        helper.assertValueEqual(fanned(helper, AllFanProcessingTypes.SMOKING, bottle, 1), 2, "slightly dirty water bottle smoked by a fan");
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
    public static void heatedBasinPurifiesUpToAcceptable(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED));
        helper.getLevel().setBlockAndUpdate(pos, AllBlocks.BASIN.getDefaultState());
        BasinBlockEntity basin = (BasinBlockEntity) helper.getLevel().getBlockEntity(pos);
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(BlueDroplets.asResource("compat/create/water_from_heated_mixing_dirty")).orElseThrow().value();
        helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null).fill(water(0, 250), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(BasinRecipe.match(basin, recipe), "dirty water does not match the heated mixing recipe");
        helper.assertTrue(BasinRecipe.apply(basin, recipe), "the heated mixing recipe did not run");
        FluidStack out = basin.getTanks().getSecond().getPrimaryHandler().getFluidInTank(0);
        helper.assertValueEqual(out.getAmount(), 250, "water out of the heated basin");
        helper.assertValueEqual(WaterPurity.getPurity(out), 2, "purity out of the heated basin");

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
        helper.assertValueEqual(WaterPurity.getPurity(bowl), 0, "purity of a terracotta bowl filled by a spout with dirty water");
        ItemStack bottle = FillingBySpout.fillItem(helper.getLevel(), 250, new ItemStack(Items.GLASS_BOTTLE), water(1, 1000));
        helper.assertValueEqual(WaterPurity.getPurity(bottle), 1, "purity of a bottle filled by a spout with slightly dirty water");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void emptyingKeepsPurity(GameTestHelper helper)
    {
        ItemStack bottle = WaterPurity.addPurity(PotionContents.createItemStack(Items.POTION, Potions.WATER), 0);
        FluidStack fromBottle = GenericItemEmptying.emptyItem(helper.getLevel(), bottle, false).getFirst();
        helper.assertTrue(bottle.isEmpty(), "the last bottle was not used up");
        helper.assertValueEqual(WaterPurity.getPurity(fromBottle), 0, "purity of water emptied from the last dirty bottle");

        ItemStack bowl = WaterPurity.addPurity(new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get()), 1);
        Pair<FluidStack, ItemStack> fromBowl = GenericItemEmptying.emptyItem(helper.getLevel(), bowl, true);
        helper.assertTrue(fromBowl.getSecond().is(ItemInit.TERRACOTTA_BOWL.get()), "emptying a terracotta water bowl gave " + fromBowl.getSecond());
        helper.assertValueEqual(fromBowl.getFirst().getAmount(), 250, "water emptied from a terracotta bowl");
        helper.assertValueEqual(WaterPurity.getPurity(fromBowl.getFirst()), 1, "purity of water emptied from a slightly dirty terracotta bowl");
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
