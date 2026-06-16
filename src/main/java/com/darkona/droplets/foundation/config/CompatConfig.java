package com.darkona.droplets.foundation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * {@code config/bluedroplets/compat.toml}: one section per optional mod; ignored when that mod is not installed.
 */
public final class CompatConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SAND_FILTER_FILTRATION_AMOUNT;
    public static final ModConfigSpec.IntValue SAND_FILTER_MB_PER_TICK;
    public static final ModConfigSpec.IntValue SAND_FILTER_MAX_PURITY;
    public static final ModConfigSpec.BooleanValue OPEN_ENDED_PIPE_PURITY;

    public static final ModConfigSpec.BooleanValue COLD_SWEAT_BODY_TEMPERATURE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> COLD_SWEAT_BODY_TEMPERATURE_CURVE;
    public static final ModConfigSpec.DoubleValue COLD_SWEAT_DRINK_COOLING;
    public static final ModConfigSpec.IntValue COLD_SWEAT_DRINK_COOLING_TICKS;

    public static final ModConfigSpec SPEC;

    static
    {
        BUILDER.push("create");
        SAND_FILTER_FILTRATION_AMOUNT = BUILDER.comment("Purification levels gained by filtering water through a Sand Filter").defineInRange("sandFilterFiltrationAmount", 1, 0, 3);
        SAND_FILTER_MB_PER_TICK = BUILDER.comment("Millibuckets of water filtered per game tick with a Sand Filter").defineInRange("sandFilterMbPerTick", 10, 1, 1000);
        SAND_FILTER_MAX_PURITY = BUILDER.comment("Highest purity a Sand Filter raises water to (0 dirty ... 3 purified); water already purer passes unchanged. Below 3, the last steps need another method (boiling, smelting)").defineInRange("sandFilterMaxPurity", 3, 0, 3);
        OPEN_ENDED_PIPE_PURITY = BUILDER.comment("Whether water pulled from the world or from a water cauldron by an open pipe end keeps its purity there, like buckets and the hose pulley; false = it reads as defaultPurity").define("openEndedPipePurity", true);
        BUILDER.pop();

        BUILDER.comment("Cold Sweat temperatures are in its own units: 0 is comfortable, 100 burning, -100 freezing").push("coldsweat");
        COLD_SWEAT_BODY_TEMPERATURE = BUILDER.comment("Whether the climate multiplier of thirst comes from Cold Sweat's body temperature (bodyTemperatureCurve) instead of the biome's temperature and downfall").define("useBodyTemperature", true);
        COLD_SWEAT_BODY_TEMPERATURE_CURVE = BUILDER.comment("With useBodyTemperature: [\"bodyTemperature,multiplier\", ...] in ascending body temperature; straight lines between points, flat beyond the ends.",
                        "Times gameplay.toml depletion.multiplier; the dimension's thirst multiplier and netherMultiplier still come first")
                .<String>defineListAllowEmpty("bodyTemperatureCurve", List.of("-100,0.8", "0,1.0", "50,1.3", "100,2.0", "150,3.0"), () -> "0,1.0", GameplayConfig::isValidPair);
        COLD_SWEAT_DRINK_COOLING = BUILDER.comment("How much drinking water (water containers, drinking by hand, the Traveler's Backpack hose) cools the body; 0 = off.",
                        "Cold Sweat's own filled waterskin is left alone: it already changes the temperature by the water it holds").defineInRange("drinkCooling", 0.0, 0.0, 100.0);
        COLD_SWEAT_DRINK_COOLING_TICKS = BUILDER.comment("0: drinkCooling lowers the body temperature once, which then drifts back with the surroundings;",
                        "more: it lowers the base temperature for this many ticks instead, like Cold Sweat's cold foods").defineInRange("drinkCoolingTicks", 0, 0, 72000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private CompatConfig() {}
}
