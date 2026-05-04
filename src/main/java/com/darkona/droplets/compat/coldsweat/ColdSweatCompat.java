package com.darkona.droplets.compat.coldsweat;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class ColdSweatCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("coldsweat");

    private ColdSweatCompat() {}

    public static double bodyTemperature(Player player)
    {
        return LOADED ? ColdSweatBridge.bodyTemperature(player) : 0;
    }
}
