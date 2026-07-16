package com.darkona.droplets.compat.vampirism;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.foundation.gui.ThirstBarStyles;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

public final class VampirismCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("vampirism");

    private VampirismCompat() {}

    public static void initClient()
    {
        if (LOADED)
            ThirstBarStyles.register(BlueDroplets.asResource("vampirism_vampire"), VampirismBridge::isVampire, ThirstBarStyles.VAMPIRE_COLOR, ThirstBarStyles.VAMPIRE_PRIORITY);
    }

    public static boolean isVampire(Player player)
    {
        return LOADED && VampirismBridge.isVampire(player);
    }
}
