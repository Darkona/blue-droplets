package com.darkona.droplets.foundation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code config/bluedroplets/compat.toml}: one section per optional mod; ignored when that mod is not installed.
 */
public final class CompatConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SAND_FILTER_FILTRATION_AMOUNT;
    public static final ModConfigSpec.IntValue SAND_FILTER_MB_PER_TICK;

    public static final ModConfigSpec.BooleanValue COLD_SWEAT_BODY_TEMPERATURE;

    public static final ModConfigSpec SPEC;

    static
    {
        BUILDER.push("create");
        SAND_FILTER_FILTRATION_AMOUNT = BUILDER.comment("Purification levels gained by filtering water through a Sand Filter").defineInRange("sandFilterFiltrationAmount", 1, 0, 3);
        SAND_FILTER_MB_PER_TICK = BUILDER.comment("Millibuckets of water filtered per game tick with a Sand Filter").defineInRange("sandFilterMbPerTick", 10, 1, 1000);
        BUILDER.pop();

        BUILDER.push("coldsweat");
        COLD_SWEAT_BODY_TEMPERATURE = BUILDER.comment("Whether the climate multiplier uses Cold Sweat's body temperature instead of the biome temperature").define("useBodyTemperature", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private CompatConfig() {}
}
