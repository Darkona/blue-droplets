package com.darkona.droplets.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * A player eats something that restores thirst, or removes it (salty, negative values): food items (anything that is
 * not drunk: no {@code UseAnim.DRINK} and not a water container), also when another mod calls {@code Player#eat}
 * directly, block foods of the {@code blue_droplets:hydrating_blocks} data map (a cake slice) and
 * {@code DropletsAPI.eat}. Posted on {@code MinecraftForge.EVENT_BUS}, on the server, once per bite. Drinks post
 * {@link DrinkEvent} instead. Food never rolls purity effects. The hydration itself also posts a
 * {@link ThirstChangeEvent} with cause {@code EAT}.
 */
public abstract class EatEvent extends PlayerEvent
{
    private final ItemStack item;

    protected EatEvent(Player player, ItemStack item)
    {
        super(player);
        this.item = item;
    }

    /**
     * The stack as it was before being eaten; empty for block foods and {@code DropletsAPI.eat}. Do not modify it.
     */
    public ItemStack getItem()
    {
        return item;
    }

    /**
     * Thirst points it gives; negative for salty food, which removes thirst (2 points = 1 droplet on the HUD).
     */
    public abstract int getThirst();

    public abstract int getQuenched();

    /**
     * Thirst points it removes: {@code -getThirst()}, or 0 when it is not salty. Points, like thirst: 2 points = 1
     * droplet on the HUD (20 points = 10 droplets). On {@link Pre}, follows {@link Pre#setThirst}.
     */
    public int getSaltiness()
    {
        return Math.max(0, -getThirst());
    }

    public boolean isSalty()
    {
        return getSaltiness() > 0;
    }

    /**
     * Before hydration. Cancel to skip it, or change what the food gives.
     */
    @Cancelable
    public static final class Pre extends EatEvent
    {
        private int thirst;
        private int quenched;

        public Pre(Player player, ItemStack item, int thirst, int quenched)
        {
            super(player, item);
            this.thirst = thirst;
            this.quenched = quenched;
        }

        @Override
        public int getThirst()
        {
            return thirst;
        }

        public void setThirst(int thirst)
        {
            this.thirst = thirst;
        }

        @Override
        public int getQuenched()
        {
            return quenched;
        }

        public void setQuenched(int quenched)
        {
            this.quenched = quenched;
        }
    }

    /**
     * After hydration, with the values that were used.
     */
    public static final class Post extends EatEvent
    {
        private final int thirst;
        private final int quenched;
        private final boolean hydrated;

        public Post(Player player, ItemStack item, int thirst, int quenched, boolean hydrated)
        {
            super(player, item);
            this.thirst = thirst;
            this.quenched = quenched;
            this.hydrated = hydrated;
        }

        @Override
        public int getThirst()
        {
            return thirst;
        }

        @Override
        public int getQuenched()
        {
            return quenched;
        }

        /**
         * Whether thirst or quenched changed.
         */
        public boolean hydrated()
        {
            return hydrated;
        }
    }
}
