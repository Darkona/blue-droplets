package com.darkona.dropletsofthirst.compat.supernatural;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.gui.ThirstBarStyles;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/**
 * Supernatural: its vampires get the red bar, and only blood hydrates them ({@code VampireThirst}; the blood bottle is
 * in {@code droplets_of_thirst:blood}). Supernatural has no bite that drinks blood: its vampires fill bottles and drink those.
 */
public final class SupernaturalCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("supernatural");

    private SupernaturalCompat() {}

    public static void initClient()
    {
        if (LOADED)
            ThirstBarStyles.register(DropletsOfThirst.asResource("supernatural_vampire"), SupernaturalBridge::isVampire, ThirstBarStyles.VAMPIRE_COLOR, ThirstBarStyles.VAMPIRE_PRIORITY);
    }

    public static boolean isVampire(Player player)
    {
        return LOADED && SupernaturalBridge.isVampire(player);
    }
}
