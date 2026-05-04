package com.darkona.droplets.compat.supernatural;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.salju.supernatural.Supernatural;
import net.salju.supernatural.events.SupernaturalManager;
import net.salju.supernatural.init.SupernaturalTags;

final class SupernaturalBridge
{
    static final ResourceLocation THIRST_ICONS = ResourceLocation.fromNamespaceAndPath(Supernatural.MODID, "textures/gui/thirst_icons.png");
    static final ResourceLocation APPLESKIN_ICONS = ResourceLocation.fromNamespaceAndPath(Supernatural.MODID, "textures/gui/appleskin_icons.png");

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

    static boolean isBlood(ItemStack stack)
    {
        return stack.is(SupernaturalTags.BLOOD);
    }
}
