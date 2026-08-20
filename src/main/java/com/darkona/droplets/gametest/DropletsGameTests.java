package com.darkona.droplets.gametest;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.event.RegisterGameTestsEvent;

/**
 * Registers the test classes that link against optional mods, only when that mod is installed; the rest are found
 * through {@code @GameTestHolder}. Tests only run with {@code forge.enabledGameTestNamespaces=blue_droplets}.
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
        if (ModList.get().isLoaded("reliquary"))
            event.register(ReliquaryTests.class);
        if (ModList.get().isLoaded("sereneseasons"))
            event.register(SereneSeasonsTests.class);
        if (ModList.get().isLoaded("farm_and_charm") && ModList.get().isLoaded("herbalbrews") && ModList.get().isLoaded("brewery"))
            event.register(DelightTests.class);
        if (ModList.get().isLoaded("miners_delight"))
            event.register(MinersDelightTests.class);
        if (ModList.get().isLoaded("miners_delight") && ModList.get().isLoaded("create"))
            event.register(MinersDelightCreateTests.class);
        if (ModList.get().isLoaded("vampirism"))
            event.register(VampirismTests.class);
    }
}
