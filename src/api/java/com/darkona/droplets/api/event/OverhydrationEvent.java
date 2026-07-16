package com.darkona.droplets.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * A player drank past full until the wasted water reached {@code overhydration.threshold}, and is about to get the
 * {@code blue_droplets:overhydrated} effect (and Nausea with {@code overhydration.nausea}). Posted on
 * {@code MinecraftForge.EVENT_BUS}, on the server. The overflow is reset afterwards, also when the event is canceled.
 * Cancel to apply nothing.
 */
@Cancelable
public class OverhydrationEvent extends PlayerEvent
{
    private final float overflow;
    private int duration;
    private int amplifier;

    public OverhydrationEvent(Player player, float overflow, int duration, int amplifier)
    {
        super(player);
        this.overflow = overflow;
        this.duration = duration;
        this.amplifier = amplifier;
    }

    /**
     * Thirst and quenched points drunk past full and not yet decayed.
     */
    public float getOverflow()
    {
        return overflow;
    }

    public int getDuration()
    {
        return duration;
    }

    public void setDuration(int duration)
    {
        this.duration = duration;
    }

    /**
     * 0 for level I; by default one level per half threshold past it, plus one if the effect is already active, at
     * most 2.
     */
    public int getAmplifier()
    {
        return amplifier;
    }

    public void setAmplifier(int amplifier)
    {
        this.amplifier = amplifier;
    }
}
