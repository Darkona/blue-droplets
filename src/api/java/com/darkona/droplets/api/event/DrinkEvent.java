package com.darkona.droplets.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * A player drinks or eats something that restores thirst. Posted on {@code NeoForge.EVENT_BUS}, on the server, once
 * per drink. The hydration itself also posts a {@link ThirstChangeEvent} with cause {@code DRINK}.
 */
public abstract class DrinkEvent extends PlayerEvent
{
    private final ItemStack item;

    protected DrinkEvent(Player player, ItemStack item)
    {
        super(player);
        this.item = item;
    }

    /**
     * The stack as it was before being used; empty when drinking by hand or through {@code DropletsAPI.drink}. Do not
     * modify it.
     */
    public ItemStack getItem()
    {
        return item;
    }

    /**
     * Before purity effects and hydration. Cancel to skip both, or change what the drink gives.
     */
    public static final class Pre extends DrinkEvent implements ICancellableEvent
    {
        private int thirst;
        private int quenched;
        private int purity;

        public Pre(Player player, ItemStack item, int thirst, int quenched, int purity)
        {
            super(player, item);
            this.thirst = thirst;
            this.quenched = quenched;
            this.purity = purity;
        }

        public int getThirst()
        {
            return thirst;
        }

        public void setThirst(int thirst)
        {
            this.thirst = thirst;
        }

        public int getQuenched()
        {
            return quenched;
        }

        public void setQuenched(int quenched)
        {
            this.quenched = quenched;
        }

        /**
         * Purity whose effects are rolled, or {@code DropletsAPI.NO_PURITY} for none.
         */
        public int getPurity()
        {
            return purity;
        }

        public void setPurity(int purity)
        {
            this.purity = purity;
        }
    }

    /**
     * After purity effects and hydration, with the values that were used.
     */
    public static final class Post extends DrinkEvent
    {
        private final int thirst;
        private final int quenched;
        private final int purity;
        private final boolean hydrated;

        public Post(Player player, ItemStack item, int thirst, int quenched, int purity, boolean hydrated)
        {
            super(player, item);
            this.thirst = thirst;
            this.quenched = quenched;
            this.purity = purity;
            this.hydrated = hydrated;
        }

        public int getThirst()
        {
            return thirst;
        }

        public int getQuenched()
        {
            return quenched;
        }

        public int getPurity()
        {
            return purity;
        }

        /**
         * Whether thirst or quenched changed: false when a purity effect prevented it or the player was already full.
         */
        public boolean hydrated()
        {
            return hydrated;
        }
    }
}
