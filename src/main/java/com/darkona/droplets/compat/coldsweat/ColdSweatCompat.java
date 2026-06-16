package com.darkona.droplets.compat.coldsweat;

import com.darkona.droplets.api.event.DrinkEvent;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ThirstComponent;
import com.darkona.droplets.foundation.config.CompatConfig;
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
 * Cold Sweat, through its public API only (GPL-3.0 with an exception for use as a library; no code of it is copied or
 * patched): body temperature for the climate multiplier and hot dirty water, drinking water cools, and its waterskin
 * carries purity. The waterskin's drink values and its purity container entry are data
 * ({@code bluedroplets:drinks}, {@code bluedroplets:purity_containers}).
 */
public final class ColdSweatCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("cold_sweat");

    /** Water taken by a waterskin, as Cold Sweat's own waterskin code takes it. */
    private static final int WATERSKIN_MB = 250;

    private ColdSweatCompat() {}

    public static void init()
    {
        if (!LOADED)
            return;
        NeoForge.EVENT_BUS.addListener(ColdSweatCompat::coolAfterDrinking);
        NeoForge.EVENT_BUS.addListener(ColdSweatCompat::fillFromWorld);
        NeoForge.EVENT_BUS.addListener(ColdSweatCompat::fillFromBlock);
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

    /**
     * Cold Sweat fills a waterskin by copying the empty one's components onto the filled one, so the empty waterskin
     * gets the purity of the water it is about to take, and loses it again once the interaction is over. This
     * handler covers a water source in sight ({@code WaterskinItem#use}).
     */
    private static void fillFromWorld(PlayerInteractEvent.RightClickItem event)
    {
        Level level = event.getLevel();
        if (level.isClientSide() || !WaterPurity.enabled() || !ColdSweatBridge.isEmptyWaterskin(event.getItemStack()))
            return;
        BlockHitResult hit = WaterPurity.pickFluid(event.getEntity(), ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK)
            return;
        FluidState fluid = level.getFluidState(hit.getBlockPos());
        if (fluid.isSource() && fluid.is(FluidTags.WATER))
            carry(level, event.getItemStack(), WaterPurity.getWaterPurity(level, hit.getBlockPos(), true));
    }

    /**
     * The same for a water cauldron or a block holding water ({@code WaterskinItem#useOn}): the first tank with enough
     * water, as Cold Sweat picks it.
     */
    private static void fillFromBlock(PlayerInteractEvent.RightClickBlock event)
    {
        Level level = event.getLevel();
        if (level.isClientSide() || !WaterPurity.enabled() || !ColdSweatBridge.isEmptyWaterskin(event.getItemStack()))
            return;
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WATER_CAULDRON) && state.getValue(BlockStateProperties.LEVEL_CAULDRON) > 0)
        {
            carry(level, event.getItemStack(), WaterPurity.cauldronPurity(level, pos));
            return;
        }
        if (level.getBlockEntity(pos) == null)
            return;
        IFluidHandler tanks = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, event.getFace());
        if (tanks == null)
            return;
        for (int tank = 0; tank < tanks.getTanks(); tank++)
        {
            FluidStack water = tanks.getFluidInTank(tank);
            if (water.is(FluidTags.WATER) && water.getAmount() >= WATERSKIN_MB)
            {
                carry(level, event.getItemStack(), WaterPurity.getPurity(water));
                return;
            }
        }
    }

    private static void carry(Level level, ItemStack emptyWaterskin, int purity)
    {
        MinecraftServer server = level.getServer();
        if (server == null)
            return;
        WaterPurity.addPurity(emptyWaterskin, purity);
        server.tell(new TickTask(server.getTickCount(), () -> emptyWaterskin.remove(ThirstComponent.PURITY)));
    }
}
