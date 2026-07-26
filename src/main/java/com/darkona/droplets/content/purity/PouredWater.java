package com.darkona.droplets.content.purity;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Water sources poured into the world (buckets, dispensers, {@code FluidUtil.tryPlaceFluid}, Create open pipe ends),
 * with the purity of the water poured, so pouring sea water into a meadow does not give clean water back. One map
 * per dimension ({@code BlockPos.asLong} to purity), saved with the world's data ({@code data/blue_droplets_poured_water.dat})
 * and never synced: only the server decides a purity. Minecraft 1.18.2 has no chunk attachments; a chunk capability
 * would save an entry in every chunk, poured water or not. Nothing runs per tick: the map is read when water is taken
 * from the world and written when it is poured.
 * <ul>
 * <li>A registered position that still holds a water source has the stored purity.</li>
 * <li>An unregistered water source next to registered ones (the infinite source between two poured buckets) has the
 * worst purity of those neighbours.</li>
 * <li>Picking up a registered source removes its entry and passes its purity to the unregistered water sources
 * beside it, so the source that refills its place is not clean either.</li>
 * <li>An entry whose position no longer holds a water source is ignored, and removed when read.</li>
 * </ul>
 */
public final class PouredWater extends SavedData
{
    private static final String NAME = "blue_droplets_poured_water";
    private static final byte NONE = -1;
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final Long2ByteOpenHashMap sources = new Long2ByteOpenHashMap();

    public PouredWater()
    {
        sources.defaultReturnValue(NONE);
    }

    /**
     * Reads a saved map ({@link #save}).
     */
    public static PouredWater load(CompoundTag tag)
    {
        PouredWater water = new PouredWater();
        long[] keys = tag.getLongArray("positions");
        byte[] purities = tag.getByteArray("purities");
        for (int i = 0; i < keys.length && i < purities.length; i++)
            water.sources.put(keys[i], purities[i]);
        return water;
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        tag.putLongArray("positions", sources.keySet().toLongArray());
        tag.putByteArray("purities", sources.values().toByteArray());
        return tag;
    }

    /**
     * The map of a dimension; created the first time water is poured there.
     */
    public static PouredWater of(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(PouredWater::load, PouredWater::new, NAME);
    }

    public boolean isEmpty()
    {
        return sources.isEmpty();
    }

    /** Entries of this dimension, for tests and debugging. */
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
        if (!(level instanceof ServerLevel server) || !WaterPurity.enabled() || !isWaterSource(level.getFluidState(pos)))
            return;
        PouredWater water = of(server);
        water.sources.put(pos.asLong(), (byte) WaterPurity.sanitizePurity(purity));
        water.setDirty();
    }

    /**
     * Purity of the water source at {@code pos} from the registry: its own entry, or the worst entry of its
     * horizontal neighbours; -1 when neither applies or there is no water source there (the world decides). Stale
     * entries met on the way are removed.
     */
    public static int purityAt(Level level, BlockPos pos)
    {
        if (existing(level) == null)
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
        PouredWater water = existing(level);
        if (water == null)
            return -1;
        if (!water.sources.containsKey(pos.asLong()))
            return worstNeighbour(level, pos);
        int purity = water.sources.remove(pos.asLong());
        water.setDirty();
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
        PouredWater water = existing(level);
        LevelChunk chunk = water == null ? null : chunk(level, pos);
        if (chunk == null)
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
            water.setDirty();
        }
        return -1;
    }

    /**
     * The map of the dimension if water was ever poured there; null on the client and in dimensions without any.
     */
    private static @Nullable PouredWater existing(Level level)
    {
        return level instanceof ServerLevel server ? server.getDataStorage().get(PouredWater::load, NAME) : null;
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
