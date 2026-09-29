package com.darkona.dropletsofthirst.api;

/**
 * Live read-only view of a player's thirst; no copy is made. On a client it holds the values last synced by the server.
 */
public interface DropletsView
{
    /** Thirst, 0 to {@link #maxThirst()}. */
    int thirst();

    /** Quenched (like food saturation): spent before thirst; never above thirst. */
    int quenched();

    /** Exhaustion towards the next point of quenched or thirst lost. */
    float exhaustion();

    default int maxThirst()
    {
        return DropletsAPI.MAX_THIRST;
    }

    /** Whether thirst is active for this player (the {@code /droplets_of_thirst enable} flag). */
    boolean isEnabled();

    /**
     * Last computed multiplier of the surroundings, armor, effects and registered {@link ExhaustionModifier}s, without
     * the {@code droplets_of_thirst:thirst_drain} attribute. Refreshed about once a second on the server; 1 on clients.
     */
    float lastModifier();
}
