package com.darkona.dropletsofthirst.compat.supernatural;

import net.minecraft.world.entity.player.Player;
import net.salju.supernatural.events.SupernaturalManager;

final class SupernaturalBridge
{
    private SupernaturalBridge() {}

    //Persistent player data: server side only.
    static boolean isVampire(Player player)
    {
        return SupernaturalManager.isVampire(player);
    }

    //Vampirism mob effect: client and server.
    static boolean hasVampirism(Player player)
    {
        return SupernaturalManager.hasVampirism(player);
    }
}
