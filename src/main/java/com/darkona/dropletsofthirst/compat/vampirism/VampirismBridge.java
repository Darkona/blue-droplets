package com.darkona.dropletsofthirst.compat.vampirism;

import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import de.teamlapen.vampirism.util.Helper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

final class VampirismBridge
{
    private VampirismBridge() {}

    static boolean isVampire(Player player)
    {
        return Helper.isVampire(player);
    }

    static void init()
    {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, VampirismBridge::drinkBlood);
    }

    /**
     * Every drink of blood by a vampire player, after other mods changed the amount. A context without an entity, item
     * or block is an altar or a command filling the blood bar, not drinking.
     */
    private static void drinkBlood(BloodDrinkEvent.PlayerDrinkBloodEvent event)
    {
        if (!(event.getVampire().getRepresentingEntity() instanceof Player player) || player.level().isClientSide || event.getAmount() <= 0)
            return;
        IDrinkBloodContext source = event.getBloodSource();
        ItemStack stack = source.getStack().orElse(ItemStack.EMPTY);
        if (stack.isEmpty() && source.getEntity().isEmpty() && source.getBlockState().isEmpty())
            return;
        VampirismCompat.bloodDrunk(player, stack, event.getAmount(), event.getSaturation());
    }
}
