package com.darkona.dropletsofthirst.compat.vampirism;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.gui.ThirstBarStyles;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class VampirismCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("vampirism");

    private VampirismCompat() {}

    public static void initClient()
    {
        if (LOADED)
            ThirstBarStyles.register(DropletsOfThirst.asResource("vampirism_vampire"), VampirismBridge::isVampire, ThirstBarStyles.VAMPIRE_COLOR, ThirstBarStyles.VAMPIRE_PRIORITY);
    }

    public static boolean isVampire(Player player)
    {
        return LOADED && VampirismBridge.isVampire(player);
    }
}
