package com.darkona.dropletsofthirst.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * A player at zero thirst is about to take dehydration damage. Posted on {@code MinecraftForge.EVENT_BUS}, on the server,
 * once per damage interval, after the difficulty's minimum health rules allowed it. Cancel to skip this hit.
 */
@Cancelable
public class DehydrationDamageEvent extends PlayerEvent
{
    private float amount;

    public DehydrationDamageEvent(Player player, float amount)
    {
        super(player);
        this.amount = amount;
    }

    public float getAmount()
    {
        return amount;
    }

    /**
     * The damage to deal; the minimum health rules are not checked again.
     */
    public void setAmount(float amount)
    {
        this.amount = amount;
    }
}
