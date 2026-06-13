package com.darkona.droplets.compat.appleskin;

import com.darkona.droplets.foundation.config.ClientConfig;
import net.neoforged.fml.ModList;

/**
 * AppleSkin, client only: with {@code followAppleSkin}, each thirst HUD visual and the tooltip icons are also turned
 * off when the player turns off AppleSkin's food counterpart, so both are set in one place. Nothing is drawn through
 * AppleSkin; without it every check here passes.
 */
public final class AppleSkinCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("appleskin");

    private AppleSkinCompat() {}

    private static boolean following()
    {
        return LOADED && ClientConfig.FOLLOW_APPLESKIN.get();
    }

    /** Quenched outline, as AppleSkin's saturation outline. */
    public static boolean quenchedOverlay()
    {
        return !following() || AppleSkinBridge.saturationOverlay();
    }

    /** Drink preview, as AppleSkin's held-food preview; {@code offhand} for a drink in the off hand only. */
    public static boolean drinkPreview(boolean offhand)
    {
        return !following() || AppleSkinBridge.foodValuesOverlay(offhand);
    }

    /** Exhaustion underlay, as AppleSkin's. */
    public static boolean exhaustionUnderlay()
    {
        return !following() || AppleSkinBridge.exhaustionUnderlay();
    }

    /** Tooltip icons, shown when AppleSkin would show food values in the tooltip (including its hold-Shift option). */
    public static boolean tooltip()
    {
        return !following() || AppleSkinBridge.foodTooltip();
    }
}
