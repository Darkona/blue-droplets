package com.darkona.droplets.gametest;

import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.PurityLevel;
import com.darkona.droplets.content.purity.PouredWater;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Poured water: water sources poured into the world keep the purity of the water poured, the infinite source between
 * two of them inherits it, and picking one up hands it on.
 */
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
        LevelChunk chunk = helper.getLevel().getChunkAt(pos);
        PouredWater water = chunk.getExistingDataOrNull(ModAttachment.POURED_WATER.get());
        return water != null && water.entries().containsKey(pos.asLong());
    }

    /** World purity of still water at {@code pos}; the tests need it above contaminated to tell poured water apart. */
    private static int worldPurity(GameTestHelper helper, BlockPos pos)
    {
        int purity = WaterPurity.getWaterPurity(helper.getLevel(), helper.getLevel().getBiome(pos), pos, true, null);
        helper.assertTrue(purity > CONTAMINATED, "world water at the test position is already contaminated");
        return purity;
    }

    private static ItemStack useFromAbove(ServerPlayer player, BlockPos water, ItemStack stack)
    {
        player.snapTo(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return TestSupport.heldResult(stack.use(player.level(), player, InteractionHand.MAIN_HAND), stack);
    }

    @GameTest(template = "box")
    public static void pouredWaterKeepsItsPurity(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        worldPurity(helper, pos);
        ServerPlayer player = TestSupport.player(helper);
        pour(helper, player, pos, CONTAMINATED);
        helper.assertTrue(registered(helper, pos), "the poured source is not registered");
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), pos), CONTAMINATED, "purity of the poured source");

        ItemStack bottle = useFromAbove(player, pos, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(bottle.is(Items.POTION), "the glass bottle was not filled");
        helper.assertValueEqual(WaterPurity.getPurity(bottle), CONTAMINATED, "purity of a bottle filled from poured water");
        helper.assertTrue(registered(helper, pos), "filling a bottle removed the entry");

        ItemStack bucket = useFromAbove(player, pos, new ItemStack(Items.BUCKET));
        helper.assertTrue(bucket.is(Items.WATER_BUCKET), "the bucket was not filled");
        helper.assertValueEqual(WaterPurity.getPurity(bucket), CONTAMINATED, "purity of a bucket filled from poured water");
        helper.assertFalse(registered(helper, pos), "picking up the source kept its entry");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void infiniteSourceInheritsAndPickingUpHandsOn(GameTestHelper helper)
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
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), middle), CONTAMINATED, "the formed source takes the worst purity beside it");

        BlockPos dispenser = west.west();
        helper.getLevel().setBlockAndUpdate(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        BlockSource source = new BlockSource(helper.getLevel(), dispenser, helper.getLevel().getBlockState(dispenser), (DispenserBlockEntity) helper.getLevel().getBlockEntity(dispenser));
        ItemStack picked = DispenserBlock.DISPENSER_REGISTRY.get(Items.BUCKET).dispense(source, new ItemStack(Items.BUCKET));
        helper.assertTrue(picked.is(Items.WATER_BUCKET), "the dispenser did not pick up the west source");
        helper.assertValueEqual(WaterPurity.getPurity(picked), CONTAMINATED, "purity of the west source picked up");
        helper.assertFalse(registered(helper, west), "the picked up source kept its entry");
        helper.assertTrue(registered(helper, middle), "picking up a poured source did not hand its purity to the source beside it");

        ItemStack last = useFromAbove(player, east, new ItemStack(Items.BUCKET));
        helper.assertValueEqual(WaterPurity.getPurity(last), PurityLevel.MURKY.level(), "purity of the east source picked up");
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), middle), CONTAMINATED, "the middle source after picking up both poured ones");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void staleEntryIsIgnored(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        int world = worldPurity(helper, pos);
        ServerPlayer player = TestSupport.player(helper);
        pour(helper, player, pos, CONTAMINATED);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        helper.assertValueEqual(WaterPurity.getWaterPurity(helper.getLevel(), pos, true), world, "a stone where water was poured");
        helper.assertFalse(registered(helper, pos), "an entry without water was kept after a read");
        helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), pos), world, "water that replaced an old entry");

        // Beside a source: an old entry next to it is not inherited either.
        BlockPos east = pos.east();
        pour(helper, player, east, CONTAMINATED);
        helper.getLevel().setBlockAndUpdate(east, Blocks.STONE.defaultBlockState());
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), pos), world, "water beside an old entry");
        helper.assertFalse(registered(helper, east), "an entry without water was kept after a neighbour read");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void dispenserPouringIsRegistered(GameTestHelper helper)
    {
        BlockPos dispenser = helper.absolutePos(new BlockPos(1, 2, 2));
        BlockPos front = dispenser.east();
        worldPurity(helper, front);
        helper.getLevel().setBlockAndUpdate(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        BlockSource source = new BlockSource(helper.getLevel(), dispenser, helper.getLevel().getBlockState(dispenser), (DispenserBlockEntity) helper.getLevel().getBlockEntity(dispenser));
        DispenserBlock.DISPENSER_REGISTRY.get(Items.WATER_BUCKET).dispense(source, waterBucket(CONTAMINATED));
        helper.assertTrue(helper.getLevel().getFluidState(front).isSource(), "the dispenser did not pour the bucket");
        helper.assertTrue(registered(helper, front), "water poured by a dispenser is not registered");
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), front), CONTAMINATED, "purity of water poured by a dispenser");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void fluidUtilPlacingIsRegistered(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        worldPurity(helper, pos);
        helper.assertTrue(FluidUtil.tryPlaceFluid(WaterPurity.waterResource(CONTAMINATED), null, helper.getLevel(), pos, false), "FluidUtil did not place the water");
        helper.assertTrue(registered(helper, pos), "water placed by FluidUtil is not registered");
        helper.assertValueEqual(WaterPurity.getBlockPurity(helper.getLevel(), pos), CONTAMINATED, "purity of water placed by FluidUtil");
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void registrySurvivesAChunkSave(GameTestHelper helper)
    {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        pour(helper, TestSupport.player(helper), pos, CONTAMINATED);
        LevelChunk chunk = helper.getLevel().getChunkAt(pos);
        PouredWater water = chunk.getExistingDataOrNull(ModAttachment.POURED_WATER.get());
        helper.assertTrue(water != null && !water.isEmpty(), "no poured water in the chunk");

        Tag saved = PouredWater.CODEC.codec().encodeStart(NbtOps.INSTANCE, water).getOrThrow();
        PouredWater loaded = PouredWater.CODEC.codec().parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertValueEqual(loaded.entries(), water.entries(), "entries after a save and a load");

        CompoundTag chunkTag = SerializableChunkData.copyOf(helper.getLevel(), chunk).write();
        CompoundTag attachments = chunkTag.getCompoundOrEmpty("neoforge:attachments");
        helper.assertTrue(attachments.contains(BlueDroplets.asResource("poured_water").toString()), "the chunk save has no poured water: " + attachments.keySet());
        helper.succeed();
    }
}
