package com.darkona.dropletsofthirst.api;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Values of an item that depend on the stack (its components): potions, filled containers, tanks. Registered for one
 * item with {@link DropletsAPI#registerDrinkProvider}.
 * <p>
 * Called on both sides, often (tooltips, the HUD, every drink): keep it cheap and return cached instances.
 */
@FunctionalInterface
public interface DrinkValueProvider
{
    /**
     * @return the values of this stack, or null when this stack gives nothing
     */
    @Nullable ThirstValues values(ItemStack stack);
}
