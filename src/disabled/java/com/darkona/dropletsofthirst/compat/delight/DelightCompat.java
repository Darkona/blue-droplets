package com.darkona.dropletsofthirst.compat.delight;

import com.darkona.dropletsofthirst.content.data.DropletsTags;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Farmer's Delight addons and Let's Do mods: kettles refuse dirty water, and the infinite water sources (taps, sinks,
 * wells) give the purity of the world's water where they stand. Recipes and drink values are data; what is code runs
 * from small mixins in {@code foundation.mixin.<mod id>} (HerbalBrews, Extra Delight, Farm & Charm) and from the click
 * event below (Brewery and any block in {@code droplets_of_thirst:rejects_dirty_water}).
 */
public final class DelightCompat
{
    private DelightCompat() {}

    public static void init()
    {
        NeoForge.EVENT_BUS.addListener(DelightCompat::rejectDirtyWater);
    }

    /**
     * Whether a kettle refuses this stack: water of a purity below {@code delight.kettleMinPurity}, with purity on.
     */
    public static boolean tooDirtyForKettle(ItemStack stack)
    {
        return WaterPurity.enabled() && WaterPurity.isWaterFilledContainer(stack) && WaterPurity.getPurity(stack) < CompatConfig.KETTLE_MIN_PURITY.get();
    }

    /**
     * Blocks in {@code droplets_of_thirst:rejects_dirty_water} are not used with water they refuse. Cancelled on both sides
     * (the purity is a synced component), so the client does not predict a fill that the server refuses.
     */
    private static void rejectDirtyWater(PlayerInteractEvent.RightClickBlock event)
    {
        if (!tooDirtyForKettle(event.getItemStack()) || !event.getLevel().getBlockState(event.getPos()).is(DropletsTags.REJECTS_DIRTY_WATER))
            return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (!event.getLevel().isClientSide())
            event.getEntity().displayClientMessage(Component.translatable("droplets_of_thirst.message.water_too_dirty"), true);
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

    /**
     * {@code water} with the purity of the source at {@code pos} when it is water without one; changes the stack.
     */
    public static FluidStack withSourcePurity(FluidStack water, Level level, BlockPos pos)
    {
        if (!water.isEmpty() && water.getFluid().isSame(Fluids.WATER) && !WaterPurity.hasPurity(water))
        {
            int purity = sourcePurity(level, pos);
            if (purity >= WaterPurity.MIN_PURITY)
                WaterPurity.addPurity(water, purity);
        }
        return water;
    }

    /**
     * Same, for the water of a block entity's own tank (Extra Delight taps and sinks); nothing before it is in a level.
     */
    public static FluidStack withSourcePurity(FluidStack water, @Nullable BlockEntity source)
    {
        return source == null || source.getLevel() == null ? water : withSourcePurity(water, source.getLevel(), source.getBlockPos());
    }
}
