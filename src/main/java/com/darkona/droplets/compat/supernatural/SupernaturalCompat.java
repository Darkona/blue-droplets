package com.darkona.droplets.compat.supernatural;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.foundation.gui.ThirstBarStyles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class SupernaturalCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("supernatural");

    private SupernaturalCompat() {}

    public static void initClient()
    {
        if (LOADED)
            ThirstBarStyles.register(BlueDroplets.asResource("supernatural_vampire"), SupernaturalBridge::hasVampirism, ThirstBarStyles.VAMPIRE_COLOR, ThirstBarStyles.VAMPIRE_PRIORITY);
    }

    public static boolean canDrinkItem(ItemStack stack, Player player)
    {
        return !LOADED || !SupernaturalBridge.isVampire(player) || SupernaturalBridge.isBlood(stack);
    }

    public static boolean isVampire(Player player)
    {
        return LOADED && SupernaturalBridge.isVampire(player);
    }

    public static boolean hasVampirism(Player player)
    {
        return LOADED && SupernaturalBridge.hasVampirism(player);
    }

    public static ResourceLocation getVampireIcons(ResourceLocation original, ItemStack stack)
    {
        return LOADED && SupernaturalBridge.isBlood(stack) ? SupernaturalBridge.THIRST_ICONS : original;
    }

    public static ResourceLocation getVampireAppleskinIcons(ResourceLocation original, Player player)
    {
        return hasVampirism(player) ? SupernaturalBridge.APPLESKIN_ICONS : original;
    }

    public static ResourceLocation getVampireAppleskinIcons(ResourceLocation original, ItemStack stack)
    {
        return LOADED && SupernaturalBridge.isBlood(stack) ? SupernaturalBridge.APPLESKIN_ICONS : original;
    }
}
