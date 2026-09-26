package com.darkona.dropletsofthirst.content.purity;

import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.stream.LongStream;

/**
 * Water sources poured into the world (buckets, dispensers, {@code FluidUtil.tryPlaceFluid}),
 * with the purity of the water poured, so pouring sea water into a meadow does not give clean water back. One map
 * per chunk ({@code BlockPos.asLong} to purity), saved with the chunk and never synced: only the server decides a
 * purity. Nothing runs per tick: the map is read when water is taken from the world and written when it is poured.
 * <ul>
 * <li>A registered position that still holds a water source has the stored purity.</li>
 * <li>An unregistered water source next to registered ones (the infinite source between two poured buckets) has the
 * worst purity of those neighbours.</li>
 * <li>Picking up a registered source removes its entry and passes its purity to the unregistered water sources
 * beside it, so the source that refills its place is not clean either.</li>
 * <li>An entry whose position no longer holds a water source is ignored, and removed when read.</li>
 * </ul>
 */
public final class PouredWater
{
    private static final byte NONE = -1;
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public static final MapCodec<PouredWater> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.LONG_STREAM.fieldOf("positions").forGetter(water -> LongStream.of(water.sources.keySet().toLongArray())),
            Codec.BYTE_BUFFER.fieldOf("purities").forGetter(water -> ByteBuffer.wrap(water.sources.values().toByteArray()))
    ).apply(instance, PouredWater::new));

    private final Long2ByteOpenHashMap sources = new Long2ByteOpenHashMap();

    public PouredWater()
    {
        sources.defaultReturnValue(NONE);
    }

    private PouredWater(LongStream positions, ByteBuffer purities)
    {
        this();
        long[] keys = positions.toArray();
        int start = purities.position();
        for (int i = 0; i < keys.length && i < purities.remaining(); i++)
            sources.put(keys[i], purities.get(start + i));
    }

    public boolean isEmpty()
    {
        return sources.isEmpty();
    }

    /** Entries of this chunk, for tests and debugging. */
    public Long2ByteMap entries()
    {
        return sources;
    }

    /**
     * Registers the water source at {@code pos} as poured water of {@code purity}. Server side, with purity on, and
     * only when the position now holds a water source.
     */
    public static void poured(Level level, BlockPos pos, int purity)
    {
        if (level.isClientSide() || !WaterPurity.enabled() || !isWaterSource(level.getFluidState(pos)))
            return;
        LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null)
            return;
        chunk.getData(ModAttachment.POURED_WATER.get()).sources.put(pos.asLong(), (byte) WaterPurity.sanitizePurity(purity));
        chunk.markUnsaved();
    }

    /**
     * Purity of the water source at {@code pos} from the registry: its own entry, or the worst entry of its
     * horizontal neighbours; -1 when neither applies or there is no water source there (the world decides). Stale
     * entries met on the way are removed.
     */
    public static int purityAt(Level level, BlockPos pos)
    {
        if (level.isClientSide())
            return -1;
        int own = stored(level, pos, true);
        if (own >= 0 || !isWaterSource(level.getFluidState(pos)))
            return own;
        return worstNeighbour(level, pos);
    }

    /**
     * The water source at {@code pos} was just picked up: returns its purity from the registry (its entry, or else the
     * worst registered neighbour), or -1. A registered source leaves its purity to the unregistered water sources
     * beside it, and its entry is removed.
     */
    public static int pickedUp(Level level, BlockPos pos)
    {
        if (level.isClientSide())
            return -1;
        LevelChunk chunk = chunk(level, pos);
        PouredWater water = chunk == null ? null : chunk.getExistingDataOrNull(ModAttachment.POURED_WATER.get());
        if (water == null || !water.sources.containsKey(pos.asLong()))
            return worstNeighbour(level, pos);
        int purity = water.sources.remove(pos.asLong());
        chunk.markUnsaved();
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        for (Direction direction : HORIZONTAL)
        {
            next.setWithOffset(pos, direction);
            if (isWaterSource(level.getFluidState(next)) && stored(level, next, false) < 0)
                poured(level, next, purity);
        }
        return purity;
    }

    private static int worstNeighbour(Level level, BlockPos pos)
    {
        int worst = -1;
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        for (Direction direction : HORIZONTAL)
        {
            next.setWithOffset(pos, direction);
            int purity = stored(level, next, true);
            if (purity >= 0 && (worst < 0 || purity < worst))
                worst = purity;
        }
        return worst;
    }

    /**
     * Entry at {@code pos} while it still holds a water source, or -1; with {@code clean}, an entry without a water
     * source is removed. Chunks that are not loaded are not loaded for it.
     */
    private static int stored(Level level, BlockPos pos, boolean clean)
    {
        LevelChunk chunk = chunk(level, pos);
        PouredWater water = chunk == null ? null : chunk.getExistingDataOrNull(ModAttachment.POURED_WATER.get());
        if (water == null)
            return -1;
        long key = pos.asLong();
        byte purity = water.sources.get(key);
        if (purity == NONE)
            return -1;
        if (isWaterSource(chunk.getFluidState(pos)))
            return purity;
        if (clean)
        {
            water.sources.remove(key);
            chunk.markUnsaved();
        }
        return -1;
    }

    private static @Nullable LevelChunk chunk(Level level, BlockPos pos)
    {
        return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    private static boolean isWaterSource(FluidState fluid)
    {
        return fluid.isSource() && fluid.is(FluidTags.WATER);
    }
}
