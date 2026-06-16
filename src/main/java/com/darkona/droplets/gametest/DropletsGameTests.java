package com.darkona.droplets.gametest;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Registers the test classes that link against optional mods, only when that mod is installed; the rest are found
 * through {@code @GameTestHolder}. Tests only run with {@code neoforge.enabledGameTestNamespaces=bluedroplets}.
 */
public final class DropletsGameTests
{
    private DropletsGameTests() {}

    public static void register(RegisterGameTestsEvent event)
    {
        if (ModList.get().isLoaded("create"))
            event.register(CreateTests.class);
        if (ModList.get().isLoaded("travelersbackpack"))
            event.register(TravelersBackpackTests.class);
        if (ModList.get().isLoaded("cold_sweat"))
            event.register(ColdSweatTests.class);
    }
}
