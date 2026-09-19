package com.darkona.dropletsofthirst.content.thirst;

import com.darkona.dropletsofthirst.compat.supernatural.SupernaturalCompat;
import com.darkona.dropletsofthirst.compat.vampirism.VampirismCompat;
import com.darkona.dropletsofthirst.content.data.DropletsTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Vampires (Vampirism, Supernatural) lose thirst like anyone, but only blood hydrates them: items in
 * {@code droplets_of_thirst:blood} with their {@code droplets_of_thirst:drinks} values, and Vampirism's blood drinking (bites,
 * bottles, containers) through its own event. Water, other drinks, food, rain and drinking by hand give them nothing.
 */
public final class VampireThirst
{
    private VampireThirst() {}

    public static boolean isVampire(Player player)
    {
        return VampirismCompat.isVampire(player) || SupernaturalCompat.isVampire(player);
    }

    public static boolean isBlood(ItemStack stack)
    {
        return stack.is(DropletsTags.BLOOD);
    }

    /** Whether this item (empty for hand drinking, block foods and the API) may hydrate this player. */
    public static boolean canHydrate(ItemStack stack, Player player)
    {
        return !isVampire(player) || isBlood(stack);
    }
}
