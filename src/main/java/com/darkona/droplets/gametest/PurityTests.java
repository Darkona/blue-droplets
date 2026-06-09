package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ItemInit;
import net.minecraft.core.BlockPos;
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
