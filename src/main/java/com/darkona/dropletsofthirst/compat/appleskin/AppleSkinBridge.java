package com.darkona.dropletsofthirst.compat.appleskin;

import net.minecraft.client.gui.screens.Screen;
import squeek.appleskin.ModConfig;

/**
 * Reads AppleSkin's client config. Only loaded through {@link AppleSkinCompat} when AppleSkin is installed.
 */
final class AppleSkinBridge
{
    private AppleSkinBridge() {}

    static boolean saturationOverlay()
    {
        return ModConfig.SHOW_SATURATION_OVERLAY.get();
    }

    static boolean foodValuesOverlay(boolean offhand)
    {
        return ModConfig.SHOW_FOOD_VALUES_OVERLAY.get() && (!offhand || ModConfig.SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND.get());
    }

    static boolean exhaustionUnderlay()
    {
        return ModConfig.SHOW_FOOD_EXHAUSTION_UNDERLAY.get();
    }

    static boolean foodTooltip()
    {
        return ModConfig.SHOW_FOOD_VALUES_IN_TOOLTIP.get() && (ModConfig.ALWAYS_SHOW_FOOD_VALUES_TOOLTIP.get() || Screen.hasShiftDown());
    }
}
