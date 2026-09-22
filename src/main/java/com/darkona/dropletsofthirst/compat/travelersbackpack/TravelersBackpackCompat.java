package com.darkona.dropletsofthirst.compat.travelersbackpack;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

/**
 * Traveler's Backpack: drinking water with the hose hydrates through its fluid effect API
 * ({@code com.tiviacz.travelersbackpack.api.fluids.EffectFluid}). Nothing here touches Traveler's Backpack classes
 * unless it is installed.
 */
public final class TravelersBackpackCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("travelersbackpack");

    private TravelersBackpackCompat() {}

    public static void init(IEventBus modBus)
    {
        if (LOADED)
            modBus.addListener(TravelersBackpackCompat::register);
    }

    /**
     * Traveler's Backpack clears its effect registry in its own common setup, so ours goes in after every setup.
     */
    private static void register(FMLLoadCompleteEvent event)
    {
        event.enqueueWork(HoseWaterEffect::register);
    }
}
