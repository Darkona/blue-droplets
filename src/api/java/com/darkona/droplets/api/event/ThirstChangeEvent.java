package com.darkona.droplets.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Thirst or quenched is about to change, or changed. Posted on {@code MinecraftForge.EVENT_BUS}, on the server, only when a
 * value actually changes: never once per tick. Values are already clamped (0..20, quenched at most thirst).
 */
public abstract class ThirstChangeEvent extends PlayerEvent
{
    public enum Cause
    {
        /** Exhaustion used up a point of quenched or thirst. */
        DEPLETION,
        /** A drink, drinking by hand, the Quenchness effect or {@code DropletsAPI.drink}. */
        DRINK,
        RAIN,
        /** Regeneration on Peaceful. */
        PEACEFUL,
        /** The {@code death.respawnThirst}/{@code respawnQuenched} options. */
        DEATH,
        COMMAND,
        /** {@code DropletsAPI.setThirst}, {@code setQuenched} or {@code addThirst}. */
        API,
        /** Food (items and block foods) or {@code DropletsAPI.eat}. */
        EAT
    }

    private final Cause cause;
    private final int oldThirst;
    private final int oldQuenched;

    protected ThirstChangeEvent(Player player, Cause cause, int oldThirst, int oldQuenched)
    {
        super(player);
        this.cause = cause;
        this.oldThirst = oldThirst;
        this.oldQuenched = oldQuenched;
    }

    public Cause getCause()
    {
        return cause;
    }

    public int getOldThirst()
    {
        return oldThirst;
    }

    public int getOldQuenched()
    {
        return oldQuenched;
    }

    /**
     * Before the change. Cancel it, or change the new values (clamped again afterwards; setting them back to the old
     * values also cancels it).
     */
    @Cancelable
    public static final class Pre extends ThirstChangeEvent
    {
        private int newThirst;
        private int newQuenched;

        public Pre(Player player, Cause cause, int oldThirst, int oldQuenched, int newThirst, int newQuenched)
        {
            super(player, cause, oldThirst, oldQuenched);
            this.newThirst = newThirst;
            this.newQuenched = newQuenched;
        }

        public int getNewThirst()
        {
            return newThirst;
        }

        public void setNewThirst(int thirst)
        {
            newThirst = thirst;
        }

        public int getNewQuenched()
        {
            return newQuenched;
        }

        public void setNewQuenched(int quenched)
        {
            newQuenched = quenched;
        }
    }

    /**
     * After the change, with the final values.
     */
    public static final class Post extends ThirstChangeEvent
    {
        private final int newThirst;
        private final int newQuenched;

        public Post(Player player, Cause cause, int oldThirst, int oldQuenched, int newThirst, int newQuenched)
        {
            super(player, cause, oldThirst, oldQuenched);
            this.newThirst = newThirst;
            this.newQuenched = newQuenched;
        }

        public int getNewThirst()
        {
            return newThirst;
        }

        public int getNewQuenched()
        {
            return newQuenched;
        }
    }
}
