package com.darkona.droplets.compat.supernatural;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.foundation.gui.ThirstBarStyles;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Supernatural: its vampires get the red bar, and only blood hydrates them ({@code VampireThirst}; the blood bottle is
 * in {@code blue_droplets:blood} through {@code #supernatural:blood}). Supernatural has no bite that drinks blood: its
 * vampires fill bottles by hitting with one in the off hand, and drink those.
 */
public final class SupernaturalCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("supernatural");

    private SupernaturalCompat() {}

    public static void initClient()
    {
        if (LOADED)
            ThirstBarStyles.register(BlueDroplets.asResource("supernatural_vampire"), SupernaturalBridge::hasVampirism, ThirstBarStyles.VAMPIRE_COLOR, ThirstBarStyles.VAMPIRE_PRIORITY);
    }

    /** On the client, from the Vampirism effect: Supernatural keeps the vampire flag in server-side player data. */
    public static boolean isVampire(Player player)
    {
        return LOADED && (player.level().isClientSide ? SupernaturalBridge.hasVampirism(player) : SupernaturalBridge.isVampire(player));
    }
}
