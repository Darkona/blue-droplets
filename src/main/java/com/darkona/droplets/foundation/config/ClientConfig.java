package com.darkona.droplets.foundation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code config/bluedroplets/client.toml}: visuals only.
 */
public final class ClientConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ONLY_SHOW_PURITY_WHEN_SHIFTING;
    public static final ModConfigSpec.BooleanValue SHOW_TOOLTIP_ICONS;
    public static final ModConfigSpec.ConfigValue<Integer> THIRST_BAR_Y_OFFSET;
    public static final ModConfigSpec.ConfigValue<Integer> THIRST_BAR_X_OFFSET;

    public static final ModConfigSpec.BooleanValue HIDE_BAR_WHEN_FULL;
    public static final ModConfigSpec.IntValue HIDE_BAR_DELAY_TICKS;
    public static final ModConfigSpec.BooleanValue SHOW_QUENCHED_OVERLAY;
    public static final ModConfigSpec.BooleanValue SHOW_DRINK_PREVIEW;
    public static final ModConfigSpec.BooleanValue SHOW_EXHAUSTION_UNDERLAY;
    public static final ModConfigSpec.BooleanValue BUFF_WAVE;

    public static final ModConfigSpec.ConfigValue<String> VAMPIRE_COLOR;
    public static final ModConfigSpec.ConfigValue<String> DEHYDRATION_COLOR;
    public static final ModConfigSpec.ConfigValue<String> OVERHYDRATED_COLOR;
    public static final ModConfigSpec.ConfigValue<String> POISON_COLOR;
    public static final ModConfigSpec.ConfigValue<String> QUENCHNESS_COLOR;
    public static final ModConfigSpec.ConfigValue<String> HYDRATED_COLOR;

    public static final ModConfigSpec SPEC;

    static
    {
        BUILDER.push("Purity tooltip");
        ONLY_SHOW_PURITY_WHEN_SHIFTING = BUILDER.comment("If the purity tooltip should be shown only when the player is pressing the shift key").define("onlyShowPurityWhenShifting", false);
        BUILDER.pop();

        BUILDER.push("Tooltip");
        SHOW_TOOLTIP_ICONS = BUILDER.comment("Show the thirst and quenched an item gives as icons in its tooltip; when off, only estimated values are shown, as text").define("showTooltipIcons", true);
        BUILDER.pop();

        BUILDER.push("Thirst Bar");
        THIRST_BAR_Y_OFFSET = BUILDER.comment("How many pixels should the thirst bar be shifted vertically from its original position").define("thirstBarYOffset", 0);
        THIRST_BAR_X_OFFSET = BUILDER.comment("How many pixels should the thirst bar be shifted horizontally from its original position").define("thirstBarXOffset", 0);
        HIDE_BAR_WHEN_FULL = BUILDER.comment("Hide the thirst bar while thirst is full (20) and the player holds nothing that restores thirst. The other bars above it move down").define("hideBarWhenFull", false);
        HIDE_BAR_DELAY_TICKS = BUILDER.comment("Ticks the bar stays visible after thirst becomes full before it is hidden (20 ticks = 1 second)").defineInRange("hideBarDelayTicks", 60, 0, 1200);
        SHOW_QUENCHED_OVERLAY = BUILDER.comment("Outline the droplets by the current quenched (like AppleSkin's saturation outline)").define("showQuenchedOverlay", true);
        SHOW_DRINK_PREVIEW = BUILDER.comment("While holding something that restores thirst, flash the thirst and quenched it would give").define("showDrinkPreview", true);
        SHOW_EXHAUSTION_UNDERLAY = BUILDER.comment("Show thirst exhaustion as a bar under the droplets (fills up until the next quenched or thirst point is lost)").define("showExhaustionUnderlay", false);
        BUFF_WAVE = BUILDER.comment("Droplets bounce one at a time, like hearts under Regeneration, while a positive thirst effect (Quenchness, Hydrated) is active").define("buffWave", true);
        BUILDER.pop();

        BUILDER.comment("Colours (#RRGGBB) of the thirst bar while a status applies; when several apply, the first in this list wins").push("Bar Colors");
        VAMPIRE_COLOR = defineColor("vampire", "Vampires (Vampirism, Supernatural)", "#B3121B");
        DEHYDRATION_COLOR = defineColor("dehydration", "Dehydration effect", "#8B5A2B");
        OVERHYDRATED_COLOR = defineColor("overhydrated", "Overhydrated effect", "#9DB0C0");
        POISON_COLOR = defineColor("poison", "Poison effect", "#7DAA3C");
        QUENCHNESS_COLOR = defineColor("quenchness", "Quenchness effect", "#5FE3FF");
        HYDRATED_COLOR = defineColor("hydrated", "Hydrated effect", "#7FE0C0");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static ModConfigSpec.ConfigValue<String> defineColor(String key, String comment, String defaultColor)
    {
        return BUILDER.comment(comment).define(key, defaultColor, value -> value instanceof String hex && hex.matches("#[0-9a-fA-F]{6}"));
    }

    private ClientConfig() {}
}
