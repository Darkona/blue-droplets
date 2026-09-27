package com.darkona.dropletsofthirst.compat.coldsweat;

import com.darkona.dropletsofthirst.api.event.DrinkEvent;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Cold Sweat, through its public API (GPL-3.0 with an exception for use as a library; no code of it is copied):
 * body temperature for the climate multiplier and hot dirty water, and drinking water cools. The waterskin's purity
 * (filled: purity of its water; emptied: none) is two small runtime mixins in {@code foundation.mixin.cold_sweat},
 * until Cold Sweat has a hook for it. The waterskin's drink values and its purity container entry are data
 * ({@code droplets_of_thirst:drinks}, {@code droplets_of_thirst:purity_containers}).
 */
public final class ColdSweatCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("cold_sweat");


    private ColdSweatCompat() {}

    public static void init()
    {
        if (!LOADED)
            return;
        NeoForge.EVENT_BUS.addListener(ColdSweatCompat::coolAfterDrinking);
    }

    public static double bodyTemperature(Player player)
    {
        return LOADED ? ColdSweatBridge.bodyTemperature(player) : 0;
    }

    /**
     * {@code coldsweat.drinkCooling}: water cools, except Cold Sweat's waterskin, which brings its own temperature.
     * Drinking by hand and the hose come with no item.
     */
    private static void coolAfterDrinking(DrinkEvent.Post event)
    {
        double cooling = CompatConfig.COLD_SWEAT_DRINK_COOLING.get();
        ItemStack item = event.getItem();
        if (cooling <= 0 || !item.isEmpty() && (!WaterPurity.isWaterFilledContainer(item) || ColdSweatBridge.isWaterskin(item)))
            return;
        ColdSweatBridge.cool(event.getEntity(), cooling, CompatConfig.COLD_SWEAT_DRINK_COOLING_TICKS.get());
    }
}
