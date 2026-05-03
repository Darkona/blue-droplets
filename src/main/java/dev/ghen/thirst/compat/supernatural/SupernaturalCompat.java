package dev.ghen.thirst.compat.supernatural;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class SupernaturalCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("supernatural");

    private SupernaturalCompat() {}

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

    public static ResourceLocation getVampireIcons(ResourceLocation original, Player player)
    {
        return hasVampirism(player) ? SupernaturalBridge.THIRST_ICONS : original;
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
