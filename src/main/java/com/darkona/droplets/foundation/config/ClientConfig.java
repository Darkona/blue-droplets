package com.darkona.droplets.foundation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code config/bluedroplets/client.toml}: visuals only.
 */
public final class ClientConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ONLY_SHOW_PURITY_WHEN_SHIFTING;
    public static final ModConfigSpec.ConfigValue<Integer> THIRST_BAR_Y_OFFSET;
    public static final ModConfigSpec.ConfigValue<Integer> THIRST_BAR_X_OFFSET;

    public static final ModConfigSpec.BooleanValue HIDE_BAR_WHEN_FULL;
    public static final ModConfigSpec.IntValue HIDE_BAR_DELAY_TICKS;

    public static final ModConfigSpec SPEC;

    static
    {
        BUILDER.push("Purity tooltip");
        ONLY_SHOW_PURITY_WHEN_SHIFTING = BUILDER.comment("If the purity tooltip should be shown only when the player is pressing the shift key").define("onlyShowPurityWhenShifting", false);
        BUILDER.pop();

        BUILDER.push("Thirst Bar");
        THIRST_BAR_Y_OFFSET = BUILDER.comment("How many pixels should the thirst bar be shifted vertically from its original position").define("thirstBarYOffset", 0);
        THIRST_BAR_X_OFFSET = BUILDER.comment("How many pixels should the thirst bar be shifted horizontally from its original position").define("thirstBarXOffset", 0);
        HIDE_BAR_WHEN_FULL = BUILDER.comment("Hide the thirst bar while thirst is full (20) and the player holds nothing that restores thirst. The other bars above it move down").define("hideBarWhenFull", false);
        HIDE_BAR_DELAY_TICKS = BUILDER.comment("Ticks the bar stays visible after thirst becomes full before it is hidden (20 ticks = 1 second)").defineInRange("hideBarDelayTicks", 60, 0, 1200);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ClientConfig() {}
}
