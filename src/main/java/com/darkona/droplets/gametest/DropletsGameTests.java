package com.darkona.droplets.gametest;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.event.RegisterGameTestsEvent;

/**
 * Registers the test classes that link against optional mods, only when that mod is installed; the rest are found
 * through {@code @GameTestHolder}. Tests only run with {@code neoforge.enabledGameTestNamespaces=blue_droplets}.
 */
public final class DropletsGameTests
{
    private DropletsGameTests() {}

    public static void register(RegisterGameTestsEvent event)
    {
        // PORT-TODO if (ModList.get().isLoaded("create"))
        //    event.register(CreateTests.class);
        if (ModList.get().isLoaded("travelersbackpack"))
            event.register(TravelersBackpackTests.class);
        if (ModList.get().isLoaded("cold_sweat"))
            event.register(ColdSweatTests.class);
        if (ModList.get().isLoaded("reliquary"))
            event.register(ReliquaryTests.class);
        if (ModList.get().isLoaded("sereneseasons"))
            event.register(SereneSeasonsTests.class);
        // PORT-TODO delight tests
    }
}
