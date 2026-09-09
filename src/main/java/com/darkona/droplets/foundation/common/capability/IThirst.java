package com.darkona.droplets.foundation.common.capability;

import net.minecraft.world.entity.player.Player;

/**
 * Internal storage of a player's thirst. Other mods read it with {@link com.darkona.droplets.api.DropletsAPI#view}
 * and change it with the {@code DropletsAPI} methods, which fire the events and keep the invariants; these setters do not.
 */
public interface IThirst
{
    int getThirst();
    void setThirst(int value);
    int getQuenched();
    void setQuenched(int value);
    float getExhaustion();
    void setExhaustion(float value);
    void addExhaustion(Player player, float amount);
    void tick(Player player);
    void updateThirstData(Player player);
    void setJustHealed();
    void ExhaustionRecalculate();
    void setShouldTickThirst(boolean value);
    boolean getShouldTickThirst();
}
