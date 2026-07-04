package com.darkona.droplets.content.purity;

import com.darkona.droplets.api.PurityLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Tint of water containers by purity, one opaque ARGB color per level. Pure functions with no render classes; the
 * client mixin on {@code ItemColors} and the terracotta bowl's color handler call them.
 */
public final class PurityTint
{
    /** Vanilla's water potion color, used for level 3 and for water without a stored purity. */
    public static final int VANILLA_WATER = 0xFF385DC6;

    /** Indexed by purity level: brown, light brown, murky grey-green, vanilla blue, light blue, near white. */
    private static final int[] COLORS = {0xFF6B4A2B, 0xFF9C7A52, 0xFF6F7F66, VANILLA_WATER, 0xFF6FC8FF, 0xFFD8F1FF};

    /** Color of a purity level; out-of-range levels read as the vanilla water color. */
    public static int colorOf(int purity)
    {
        return purity >= PurityLevel.MIN && purity <= PurityLevel.MAX ? COLORS[purity] : VANILLA_WATER;
    }

    /**
     * The color to draw the stack's liquid layer with: the purity color for a water container that stores a purity,
     * else {@code original} untouched (other potions, other layers, purity off, no stored purity).
     */
    public static int color(ItemStack stack, int tintIndex, int original)
    {
        if (tintIndex != 0 || !WaterPurity.enabled() || !WaterPurity.isWaterFilledContainer(stack) || !WaterPurity.hasPurity(stack))
            return original;
        return colorOf(WaterPurity.getPurity(stack));
    }

    private PurityTint() {}
}
