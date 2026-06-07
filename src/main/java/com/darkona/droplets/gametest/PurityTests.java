package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.PurityConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Purity of cauldrons (rain and dripstone mixing, boiling) and of water in the world.
 */
@GameTestHolder(BlueDroplets.ID)
@PrefixGameTestTemplate(false)
public class PurityTests
{
    private static BlockState cauldron(int purity)
    {
        return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2).setValue(WaterPurity.BLOCK_PURITY, purity + 1);
    }

    @GameTest(template = "empty")
    public static void naturalFillKeepsTheLowerPurity(GameTestHelper helper)
    {
        BlockState filled = cauldron(3).setValue(LayeredCauldronBlock.LEVEL, 3);
        helper.assertValueEqual(WaterPurity.getBlockPurity(WaterPurity.naturalFill(cauldron(3), filled, 1)), 1, "purified cauldron topped up with purity 1 water");
        helper.assertValueEqual(WaterPurity.getBlockPurity(WaterPurity.naturalFill(cauldron(0), filled, 3)), 0, "dirty cauldron topped up with purity 3 water");
        helper.assertValueEqual(WaterPurity.naturalFill(cauldron(0), filled, -1), filled, "configured -1 changes nothing");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void boilingNeedsTheOptionAndHeat(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pos, cauldron(1));
        RandomSource alwaysLow = RandomSource.create(0);
        boolean boiling = PurityConfig.CAULDRON_BOILING.get();
        double chance = PurityConfig.CAULDRON_BOILING_CHANCE.get();
        try
        {
            PurityConfig.CAULDRON_BOILING.set(false);
            WaterPurity.boil(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, alwaysLow);
            helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel().getBlockState(pos)), 1, "purity with cauldron.boiling off");
            PurityConfig.CAULDRON_BOILING.set(true);
            PurityConfig.CAULDRON_BOILING_CHANCE.set(1.0);
            WaterPurity.boil(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, alwaysLow);
            helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel().getBlockState(pos)), 2, "purity after boiling once on a lit campfire");
            helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            WaterPurity.boil(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, alwaysLow);
            helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel().getBlockState(pos)), 2, "purity after boiling without heat");
        }
        finally
        {
            PurityConfig.CAULDRON_BOILING.set(boiling);
            PurityConfig.CAULDRON_BOILING_CHANCE.set(chance);
        }
        helper.succeed();
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
