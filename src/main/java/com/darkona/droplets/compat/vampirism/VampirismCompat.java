package com.darkona.droplets.compat.vampirism;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class VampirismCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("vampirism");

    private VampirismCompat() {}

    public static boolean isVampire(Player player)
    {
        return LOADED && VampirismBridge.isVampire(player);
    }
}
