package com.darkona.dropletsofthirst.compat.coldsweat;

import com.momosoftworks.coldsweat.api.temperature.modifier.FoodTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import com.momosoftworks.coldsweat.core.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class ColdSweatBridge
{
    private ColdSweatBridge() {}

    static double bodyTemperature(Player player)
    {
        return Temperature.get(player, Temperature.Trait.BODY);
    }

    static boolean isWaterskin(ItemStack stack)
    {
        return stack.is(ModItems.FILLED_WATERSKIN);
    }

    static boolean isEmptyWaterskin(ItemStack stack)
    {
        return stack.is(ModItems.WATERSKIN);
    }

    /**
     * {@code ticks == 0}: lowers the core temperature once. Otherwise lowers the base temperature for {@code ticks},
     * replacing an earlier drink's modifier instead of stacking with it.
     */
    static void cool(Player player, double amount, int ticks)
    {
        if (ticks <= 0)
        {
            Temperature.add(player, Temperature.Trait.CORE, -amount);
            return;
        }
        FoodTempModifier modifier = new FoodTempModifier(-amount);
        modifier.getNBT().putString("item", "droplets_of_thirst:water");
        modifier.expires(ticks);
        Temperature.replaceOrAddModifier(player, modifier, Temperature.Trait.BASE, Matcher.EQUALS);
    }
}
