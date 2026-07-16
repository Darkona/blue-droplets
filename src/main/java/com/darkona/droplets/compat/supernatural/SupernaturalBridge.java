package com.darkona.droplets.compat.supernatural;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.salju.supernatural.events.SupernaturalManager;
import net.salju.supernatural.init.SupernaturalItems;

final class SupernaturalBridge
{
    private SupernaturalBridge() {}

    static boolean isVampire(Player player)
    {
        return SupernaturalManager.isVampire(player);
    }

    static boolean isBlood(ItemStack stack)
    {
        return stack.is(SupernaturalItems.BLOOD.get());
    }
}
