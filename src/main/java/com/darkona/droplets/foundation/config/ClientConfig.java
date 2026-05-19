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

    public static final ModConfigSpec SPEC;

    static
    {
        BUILDER.push("Purity tooltip");
        ONLY_SHOW_PURITY_WHEN_SHIFTING = BUILDER.comment("If the purity tooltip should be shown only when the player is pressing the shift key").define("onlyShowPurityWhenShifting", false);
        BUILDER.pop();

        BUILDER.push("Thirst Bar");
        THIRST_BAR_Y_OFFSET = BUILDER.comment("How many pixels should the thirst bar be shifted vertically from its original position").define("thirstBarYOffset", 0);
        THIRST_BAR_X_OFFSET = BUILDER.comment("How many pixels should the thirst bar be shifted horizontally from its original position").define("thirstBarXOffset", 0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ClientConfig() {}
}
