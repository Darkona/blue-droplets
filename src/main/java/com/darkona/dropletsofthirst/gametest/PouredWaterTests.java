package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.api.PurityLevel;
import com.darkona.dropletsofthirst.content.purity.PouredWater;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.foundation.config.PurityConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.darkona.dropletsofthirst.gametest.TestSupport.assertValueEqual;

/**
 * Poured water: water sources poured into the world keep the purity of the water poured, the infinite source between
 * two of them inherits it, and picking one up hands it on.
 */
@GameTestHolder(DropletsOfThirst.ID)
@PrefixGameTestTemplate(false)
public class PouredWaterTests
{
    private static final int CONTAMINATED = PurityLevel.CONTAMINATED.level();

    private static ItemStack waterBucket(int purity)
    {
        return WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), purity);
    }

    /** Empties a water bucket of {@code purity} at {@code pos} as a player does. */
    private static void pour(GameTestHelper helper, ServerPlayer player, BlockPos pos, int purity)
    {
        ItemStack bucket = waterBucket(purity);
        helper.assertTrue(((BucketItem) Items.WATER_BUCKET).emptyContents(player, helper.getLevel(), pos, null, bucket), "the bucket was not emptied at " + pos);
        helper.assertTrue(helper.getLevel().getFluidState(pos).isSource(), "no water source where the bucket was emptied");
    }

    private static boolean registered(GameTestHelper helper, BlockPos pos)
    {
        return PouredWater.of(helper.getLevel()).entries().containsKey(pos.asLong());
    }

    /** World purity of still water at {@code pos}; the tests need it above contaminated to tell poured water apart. */
    private static int worldPurity(GameTestHelper helper, BlockPos pos)
    {
        List<String> trace = new ArrayList<>();
        int purity = WaterPurity.getWaterPurity(helper.getLevel(), helper.getLevel().getBiome(pos), pos, true, trace);
        helper.assertTrue(purity > CONTAMINATED, "world water at the test position is already contaminated: " + trace + " in " + helper.getLevel().getBiome(pos).unwrapKey());
        return purity;
    }

    /**
     * The game test world of 1.20.1 is not a plains superflat: its biomes can be oceans, whose water is always
     * contaminated. The tests need world water above that, so the salt water rule is off while they run.
     */
    private static void withoutSaltWater(Runnable test)
    {
        int salt = PurityConfig.SALT_WATER_PURITY.get();
        try
        {
            TestSupport.set(PurityConfig.SALT_WATER_PURITY, -1);
            test.run();
        }
        finally
        {
            TestSupport.set(PurityConfig.SALT_WATER_PURITY, salt);
        }
    }

    private static ItemStack useFromAbove(ServerPlayer player, BlockPos water, ItemStack stack)
    {
        player.moveTo(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.use(player.level(), player, InteractionHand.MAIN_HAND).getObject();
    }

    @GameTest(template = "box")
    public static void pouredWaterKeepsItsPurity(GameTestHelper helper)
    {
        withoutSaltWater(() -> pouredWaterKeepsItsPurityInLevel(helper));
    }

    private static void pouredWaterKeepsItsPurityInLevel(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        worldPurity(helper, pos);
        ServerPlayer player = TestSupport.player(helper);
        pour(helper, player, pos, CONTAMINATED);
        helper.assertTrue(registered(helper, pos), "the poured source is not registered");
        assertValueEqual(helper, WaterPurity.getBlockPurity(helper.getLevel(), pos), CONTAMINATED, "purity of the poured source");

        ItemStack bottle = useFromAbove(player, pos, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(bottle.is(Items.POTION), "the glass bottle was not filled");
        assertValueEqual(helper, WaterPurity.getPurity(bottle), CONTAMINATED, "purity of a bottle filled from poured water");
        helper.assertTrue(registered(helper, pos), "filling a bottle removed the entry");

        ItemStack bucket = useFromAbove(player, pos, new ItemStack(Items.BUCKET));
        helper.assertTrue(bucket.is(Items.WATER_BUCKET), "the bucket was not filled");
        assertValueEqual(helper, WaterPurity.getPurity(bucket), CONTAMINATED, "purity of a bucket filled from poured water");
        helper.assertFalse(registered(helper, pos), "picking up the source kept its entry");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void infiniteSourceInheritsAndPickingUpHandsOn(GameTestHelper helper)
    {
        withoutSaltWater(() -> infiniteSourceInheritsAndPickingUpHandsOnInLevel(helper));
    }

    private static void infiniteSourceInheritsAndPickingUpHandsOnInLevel(GameTestHelper helper)
    {
        BlockPos west = helper.absolutePos(new BlockPos(1, 2, 2));
        BlockPos middle = west.east();
        BlockPos east = middle.east();
        worldPurity(helper, middle);
        ServerPlayer player = TestSupport.player(helper);
        pour(helper, player, west, CONTAMINATED);
        pour(helper, player, east, PurityLevel.MURKY.level());
        // The source two poured sources form between them, without waiting for the water to flow.
        helper.getLevel().setBlockAndUpdate(middle, Blocks.WATER.defaultBlockState());
        helper.assertFalse(registered(helper, middle), "the formed source is registered");
        assertValueEqual(helper, WaterPurity.getBlockPurity(helper.getLevel(), middle), CONTAMINATED, "the formed source takes the worst purity beside it");

        BlockPos dispenser = west.west();
        helper.getLevel().setBlockAndUpdate(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        // A real dispense (the dispenser's scheduled tick): the purity goes on as the filled bucket goes back in the slot.
        DispenserBlockEntity dispenserEntity = (DispenserBlockEntity) helper.getLevel().getBlockEntity(dispenser);
        dispenserEntity.setItem(0, new ItemStack(Items.BUCKET));
        helper.getLevel().getBlockState(dispenser).tick(helper.getLevel(), dispenser, helper.getLevel().random);
        ItemStack picked = dispenserEntity.getItem(0);
        helper.assertTrue(picked.is(Items.WATER_BUCKET), "the dispenser did not pick up the west source");
        assertValueEqual(helper, WaterPurity.getPurity(picked), CONTAMINATED, "purity of the west source picked up");
        helper.assertFalse(registered(helper, west), "the picked up source kept its entry");
        helper.assertTrue(registered(helper, middle), "picking up a poured source did not hand its purity to the source beside it");

        ItemStack last = useFromAbove(player, east, new ItemStack(Items.BUCKET));
        assertValueEqual(helper, WaterPurity.getPurity(last), PurityLevel.MURKY.level(), "purity of the east source picked up");
        assertValueEqual(helper, WaterPurity.getBlockPurity(helper.getLevel(), middle), CONTAMINATED, "the middle source after picking up both poured ones");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void staleEntryIsIgnored(GameTestHelper helper)
    {
        withoutSaltWater(() -> staleEntryIsIgnoredInLevel(helper));
    }

    private static void staleEntryIsIgnoredInLevel(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        int world = worldPurity(helper, pos);
        ServerPlayer player = TestSupport.player(helper);
        pour(helper, player, pos, CONTAMINATED);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        assertValueEqual(helper, WaterPurity.getWaterPurity(helper.getLevel(), pos, true), world, "a stone where water was poured");
        helper.assertFalse(registered(helper, pos), "an entry without water was kept after a read");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        assertValueEqual(helper, WaterPurity.getBlockPurity(helper.getLevel(), pos), world, "water that replaced an old entry");

        // Beside a source: an old entry next to it is not inherited either.
        BlockPos east = pos.east();
        pour(helper, player, east, CONTAMINATED);
        helper.getLevel().setBlockAndUpdate(east, Blocks.STONE.defaultBlockState());
        assertValueEqual(helper, WaterPurity.getBlockPurity(helper.getLevel(), pos), world, "water beside an old entry");
        helper.assertFalse(registered(helper, east), "an entry without water was kept after a neighbour read");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void dispenserPouringIsRegistered(GameTestHelper helper)
    {
        withoutSaltWater(() -> dispenserPouringIsRegisteredInLevel(helper));
    }

    private static void dispenserPouringIsRegisteredInLevel(GameTestHelper helper)
    {
        BlockPos dispenser = helper.absolutePos(new BlockPos(1, 2, 2));
        BlockPos front = dispenser.east();
        worldPurity(helper, front);
        helper.getLevel().setBlockAndUpdate(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity entity = (DispenserBlockEntity) helper.getLevel().getBlockEntity(dispenser);
        entity.setItem(0, waterBucket(CONTAMINATED));
        helper.getLevel().getBlockState(dispenser).tick(helper.getLevel(), dispenser, helper.getLevel().random);
        helper.assertTrue(helper.getLevel().getFluidState(front).isSource(), "the dispenser did not pour the bucket");
        helper.assertTrue(registered(helper, front), "water poured by a dispenser is not registered");
        assertValueEqual(helper, WaterPurity.getBlockPurity(helper.getLevel(), front), CONTAMINATED, "purity of water poured by a dispenser");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void fluidUtilPlacingIsRegistered(GameTestHelper helper)
    {
        withoutSaltWater(() -> fluidUtilPlacingIsRegisteredInLevel(helper));
    }

    private static void fluidUtilPlacingIsRegisteredInLevel(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        worldPurity(helper, pos);
        FluidTank tank = new FluidTank(1000);
        tank.fill(WaterPurity.addPurity(new FluidStack(Fluids.WATER, 1000), CONTAMINATED), FluidTank.FluidAction.EXECUTE);
        helper.assertTrue(FluidUtil.tryPlaceFluid(null, helper.getLevel(), InteractionHand.MAIN_HAND, pos, tank, tank.getFluid().copy()), "FluidUtil did not place the water");
        helper.assertTrue(registered(helper, pos), "water placed by FluidUtil is not registered");
        assertValueEqual(helper, WaterPurity.getBlockPurity(helper.getLevel(), pos), CONTAMINATED, "purity of water placed by FluidUtil");
        helper.succeed();
    }

    /**
     * 1.20.1 keeps the registry of a dimension in its saved data ({@code data/droplets_of_thirst_poured_water.dat}) instead of
     * a chunk attachment: pouring marks it for saving, and what it saves loads back the same.
     */
    @GameTest(template = "box")
    public static void registrySurvivesASave(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        pour(helper, TestSupport.player(helper), pos, CONTAMINATED);
        PouredWater water = PouredWater.of(helper.getLevel());
        helper.assertFalse(water.isEmpty(), "no poured water in the dimension");
        helper.assertTrue(water.isDirty(), "pouring did not mark the registry for saving");

        CompoundTag saved = water.save(new CompoundTag());
        PouredWater loaded = PouredWater.load(saved);
        assertValueEqual(helper, loaded.entries(), water.entries(), "entries after a save and a load");
        helper.succeed();
    }
}
