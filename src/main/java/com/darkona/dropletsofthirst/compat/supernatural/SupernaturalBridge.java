package com.darkona.dropletsofthirst.compat.supernatural;

import net.minecraft.world.entity.player.Player;
import net.salju.supernatural.events.SupernaturalManager;

final class SupernaturalBridge
{
    private SupernaturalBridge() {}

    static boolean isVampire(Player player)
    {
        return SupernaturalManager.isVampire(player);
    }
}
