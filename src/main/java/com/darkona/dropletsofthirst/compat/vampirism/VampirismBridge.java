package com.darkona.dropletsofthirst.compat.vampirism;

import de.teamlapen.vampirism.util.Helper;
import net.minecraft.world.entity.player.Player;

final class VampirismBridge
{
    private VampirismBridge() {}

    static boolean isVampire(Player player)
    {
        return Helper.isVampire(player);
    }
}
