package com.darkona.dropletsofthirst.compat.supernatural;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.gui.ThirstBarStyles;
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
            ThirstBarStyles.register(DropletsOfThirst.asResource("supernatural_vampire"), SupernaturalBridge::hasVampirism, ThirstBarStyles.VAMPIRE_COLOR, ThirstBarStyles.VAMPIRE_PRIORITY);
    }

    public static boolean canDrinkItem(ItemStack stack, Player player)
    {
        return !LOADED || !SupernaturalBridge.isVampire(player) || SupernaturalBridge.isBlood(stack);
    }

    public static boolean isVampire(Player player)
    {
        return LOADED && SupernaturalBridge.isVampire(player);
    }
}
