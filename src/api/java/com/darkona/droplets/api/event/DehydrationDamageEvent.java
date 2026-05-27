package com.darkona.droplets.api.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * A player at zero thirst is about to take dehydration damage. Posted on {@code NeoForge.EVENT_BUS}, on the server,
 * once per damage interval, after the difficulty's minimum health rules allowed it. Cancel to skip this hit.
 */
public class DehydrationDamageEvent extends PlayerEvent implements ICancellableEvent
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
