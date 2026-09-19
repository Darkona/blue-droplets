package com.darkona.dropletsofthirst.api;

import net.minecraft.world.entity.player.Player;

/**
 * Changes how fast a player loses thirst, for what the {@code droplets_of_thirst:thirst_drain} attribute cannot express
 * (additive terms, climate or season rules). Prefer the attribute for equipment and effects.
 * <p>
 * Evaluated on the server about once a second per player and when armor, effects or the dimension change (see
 * {@link DropletsAPI#refreshExhaustionModifier}), never every tick; the result is cached.
 */
@FunctionalInterface
public interface ExhaustionModifier
{
    /**
     * @param multiplier the multiplier so far: Droplets of Thirst's own factors and the modifiers registered before this one
     *                   (in id order)
     * @return the new multiplier; values below 0 count as 0
     */
    float apply(Player player, float multiplier);
}
