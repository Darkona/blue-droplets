package com.darkona.droplets.compat.delight;

import com.darkona.droplets.content.data.DropletsTags;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Farmer's Delight addons: blocks in {@code blue_droplets:rejects_dirty_water} refuse dirty water, and infinite water
 * sources give the purity of the world's water where they stand. Recipes and drink values are data; what is code runs
 * from small mixins in {@code foundation.mixin.<mod id>} (Miner's Delight on 1.19.2) and from the click event below.
 */
public final class DelightCompat
{
    private DelightCompat() {}

    public static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(DelightCompat::rejectDirtyWater);
    }

    /**
     * Whether a kettle refuses this stack: water of a purity below {@code delight.kettleMinPurity}, with purity on.
     */
    public static boolean tooDirtyForKettle(ItemStack stack)
    {
        return WaterPurity.enabled() && WaterPurity.isWaterFilledContainer(stack) && WaterPurity.getPurity(stack) < CompatConfig.KETTLE_MIN_PURITY.get();
    }

    /**
     * Blocks in {@code blue_droplets:rejects_dirty_water} are not used with water they refuse. Cancelled on both sides
     * (the purity is item NBT, synced to the client), so the client does not predict a fill that the server refuses.
     */
    private static void rejectDirtyWater(PlayerInteractEvent.RightClickBlock event)
    {
        if (!tooDirtyForKettle(event.getItemStack()) || !event.getLevel().getBlockState(event.getPos()).is(DropletsTags.REJECTS_DIRTY_WATER))
            return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!event.getLevel().isClientSide())
            event.getEntity().displayClientMessage(Component.translatable("blue_droplets.message.water_too_dirty"), true);
    }

    /**
     * Purity of water from an infinite source at {@code pos}: the world's water there, as from a source block; -1 when
     * {@code delight.worldPurityWaterSources} or purity is off.
     */
    public static int sourcePurity(@Nullable Level level, BlockPos pos)
    {
        if (level == null || !WaterPurity.enabled() || !CompatConfig.WORLD_PURITY_WATER_SOURCES.get())
            return -1;
        return WaterPurity.getWaterPurity(level, pos, true);
    }

    /**
     * {@code filled} with the purity of the source at {@code pos} when it is a water container without one.
     */
    public static ItemStack withSourcePurity(ItemStack filled, Level level, BlockPos pos)
    {
        if (WaterPurity.isWaterFilledContainer(filled) && !WaterPurity.hasPurity(filled))
        {
            int purity = sourcePurity(level, pos);
            if (purity >= WaterPurity.MIN_PURITY)
                WaterPurity.addPurity(filled, purity);
        }
        return filled;
    }
}
