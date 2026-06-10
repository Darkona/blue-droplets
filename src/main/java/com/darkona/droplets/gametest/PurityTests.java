package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Purity of water taken from cauldrons (1, or 2 on a heat source) and of water in the world.
 */
@GameTestHolder(BlueDroplets.ID)
@PrefixGameTestTemplate(false)
public class PurityTests
{
    @GameTest(template = "box")
    public static void cauldronPurityDependsOnHeat(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), pos), WaterPurity.CAULDRON_PURITY, "cauldron on stone");
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), pos), WaterPurity.HEATED_CAULDRON_PURITY, "cauldron on a lit campfire");
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false));
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), pos), WaterPurity.CAULDRON_PURITY, "cauldron on an unlit campfire");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void itemsTakeTheCauldronPurity(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack bottle = useOnFullCauldron(helper, player, pos, new ItemStack(Items.GLASS_BOTTLE, 3));
        helper.assertValueEqual(bottle.getCount(), 2, "glass bottles left in hand");
        ItemStack water = ItemStack.EMPTY;
        for (ItemStack item : player.getInventory().items)
            if (item.is(Items.POTION))
                water = item;
        helper.assertFalse(water.isEmpty(), "no water bottle in the inventory");
        helper.assertTrue(WaterPurity.hasPurity(water), "water bottle from a cauldron has no purity stored");
        helper.assertValueEqual(WaterPurity.getPurity(water), WaterPurity.HEATED_CAULDRON_PURITY, "water bottle from a heated cauldron");

        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        ItemStack bucket = useOnFullCauldron(helper, player, pos, new ItemStack(Items.BUCKET));
        helper.assertTrue(bucket.is(Items.WATER_BUCKET), "the bucket was not filled");
        helper.assertValueEqual(WaterPurity.getPurity(bucket), WaterPurity.CAULDRON_PURITY, "water bucket from a cauldron");

        ItemStack bowl = useOnFullCauldron(helper, player, pos, new ItemStack(ItemInit.TERRACOTTA_BOWL.get()));
        helper.assertTrue(bowl.is(ItemInit.TERRACOTTA_WATER_BOWL.get()), "the terracotta bowl was not filled");
        helper.assertValueEqual(WaterPurity.getPurity(bowl), WaterPurity.CAULDRON_PURITY, "terracotta water bowl from a cauldron");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void fluidCapabilityCarriesTheCauldronPurity(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        helper.assertTrue(handler != null, "a water cauldron has no fluid handler");
        int heated = WaterPurity.HEATED_CAULDRON_PURITY;

        FluidStack inTank = handler.getFluidInTank(0);
        helper.assertTrue(WaterPurity.hasPurity(inTank), "tank contents have no purity");
        helper.assertValueEqual(WaterPurity.getPurity(inTank), heated, "purity of the tank contents");
        FluidStack simulated = handler.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        helper.assertValueEqual(WaterPurity.getPurity(simulated), heated, "purity of a simulated drain");

        FluidStack other = WaterPurity.addPurity(new FluidStack(Fluids.WATER, 1000), WaterPurity.CAULDRON_PURITY);
        helper.assertTrue(handler.drain(other, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "drained water of another purity");
        FluidStack drained = handler.drain(inTank.copyWithAmount(1000), IFluidHandler.FluidAction.EXECUTE);
        helper.assertValueEqual(drained.getAmount(), 1000, "amount drained asking for the tank contents");
        helper.assertValueEqual(WaterPurity.getPurity(drained), heated, "purity of the drained water");
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.CAULDRON), "the cauldron was not emptied");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bucketsKeepPurityThroughTheFluidCapability(GameTestHelper helper)
    {
        ItemStack bucket = WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), 0);
        FluidStack inBucket = FluidUtil.getFluidContained(bucket).orElse(FluidStack.EMPTY);
        helper.assertTrue(inBucket.is(Fluids.WATER), "no water in a water bucket");
        helper.assertTrue(WaterPurity.hasPurity(inBucket), "water read from a bucket has no purity");
        helper.assertValueEqual(WaterPurity.getPurity(inBucket), 0, "purity of water read from a bucket");

        ItemStack filled = FluidUtil.getFilledBucket(WaterPurity.addPurity(new FluidStack(Fluids.WATER, 1000), 3));
        helper.assertTrue(filled.is(Items.WATER_BUCKET), "no water bucket for water with a purity");
        helper.assertValueEqual(WaterPurity.getPurity(filled), 3, "purity of a bucket filled with water");
        helper.assertFalse(WaterPurity.hasPurity(FluidUtil.getFilledBucket(new FluidStack(Fluids.WATER, 1000))), "a bucket of water without purity got one");
        helper.succeed();
    }

    private static ItemStack useOnFullCauldron(GameTestHelper helper, Player player, BlockPos pos, ItemStack stack)
    {
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    @GameTest(template = "box")
    public static void worldWaterHasAValidPurity(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        int purity = WaterPurity.getBlockPurity(helper.getLevel(), pos);
        helper.assertTrue(purity >= WaterPurity.MIN_PURITY && purity <= WaterPurity.MAX_PURITY, "world water purity " + purity);
        helper.succeed();
    }
}
