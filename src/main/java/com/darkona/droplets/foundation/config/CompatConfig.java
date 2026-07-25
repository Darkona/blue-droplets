package com.darkona.droplets.foundation.config;

import com.darkona.droplets.api.PurityLevel;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * {@code config/blue_droplets/compat.toml}: one section per optional mod; ignored when that mod is not installed.
 */
public final class CompatConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue SAND_FILTER_FILTRATION_AMOUNT;
    public static final ForgeConfigSpec.IntValue SAND_FILTER_MB_PER_TICK;
    public static final ForgeConfigSpec.IntValue SAND_FILTER_MAX_PURITY;
    public static final ForgeConfigSpec.BooleanValue OPEN_ENDED_PIPE_PURITY;

    public static final ForgeConfigSpec.BooleanValue COLD_SWEAT_BODY_TEMPERATURE;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> COLD_SWEAT_BODY_TEMPERATURE_CURVE;
    public static final ForgeConfigSpec.DoubleValue COLD_SWEAT_DRINK_COOLING;
    public static final ForgeConfigSpec.IntValue COLD_SWEAT_DRINK_COOLING_TICKS;

    public static final ForgeConfigSpec.BooleanValue SERENE_SEASONS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SERENE_SEASONS_TROPICAL_DRY;
    public static final ForgeConfigSpec.DoubleValue SERENE_SEASONS_TROPICAL_WET;
    public static final ForgeConfigSpec.DoubleValue SERENE_SEASONS_SPRING;
    public static final ForgeConfigSpec.DoubleValue SERENE_SEASONS_SUMMER;
    public static final ForgeConfigSpec.DoubleValue SERENE_SEASONS_AUTUMN;
    public static final ForgeConfigSpec.DoubleValue SERENE_SEASONS_WINTER;

    public static final ForgeConfigSpec.IntValue RELIQUARY_EMPEROR_CHALICE_COOLDOWN;

    public static final ForgeConfigSpec.IntValue KETTLE_MIN_PURITY;
    public static final ForgeConfigSpec.BooleanValue WORLD_PURITY_WATER_SOURCES;

    public static final ForgeConfigSpec SPEC;

    static
    {
        BUILDER.push("create");
        SAND_FILTER_FILTRATION_AMOUNT = BUILDER.comment("Purification levels gained by filtering water through a Sand Filter").defineInRange("sandFilterFiltrationAmount", 1, 0, PurityLevel.MAX);
        SAND_FILTER_MB_PER_TICK = BUILDER.comment("Millibuckets of water filtered per game tick with a Sand Filter").defineInRange("sandFilterMbPerTick", 10, 1, 1000);
        SAND_FILTER_MAX_PURITY = BUILDER.comment("Highest purity a Sand Filter raises water to (0 contaminated ... 5 pure); water already purer passes unchanged.",
                        "With Create, filters are the only way to pure water: cooking stops at clean (4)").defineInRange("sandFilterMaxPurity", PurityLevel.PURE.level(), PurityLevel.MIN, PurityLevel.MAX);
        OPEN_ENDED_PIPE_PURITY = BUILDER.comment("Whether water pulled from the world or from a water cauldron by an open pipe end keeps its purity there, like buckets and the hose pulley; false = it reads as defaultPurity").define("openEndedPipePurity", true);
        BUILDER.pop();

        BUILDER.comment("Cold Sweat temperatures are in its own units: 0 is comfortable, 100 burning, -100 freezing").push("coldsweat");
        COLD_SWEAT_BODY_TEMPERATURE = BUILDER.comment("Whether the climate multiplier of thirst comes from Cold Sweat's body temperature (bodyTemperatureCurve) instead of the biome's temperature and downfall").define("useBodyTemperature", true);
        COLD_SWEAT_BODY_TEMPERATURE_CURVE = BUILDER.comment("With useBodyTemperature: [\"bodyTemperature,multiplier\", ...] in ascending body temperature; straight lines between points, flat beyond the ends.",
                        "Times gameplay.toml depletion.multiplier; the dimension's thirst multiplier and netherMultiplier still come first")
                .<String>defineListAllowEmpty(List.of("bodyTemperatureCurve"), () -> List.of("-100,0.8", "0,1.0", "50,1.3", "100,2.0", "150,3.0"), GameplayConfig::isValidPair);
        COLD_SWEAT_DRINK_COOLING = BUILDER.comment("How much drinking water (water containers, drinking by hand, the Traveler's Backpack hose) cools the body; 0 = off.",
                        "Cold Sweat's own filled waterskin is left alone: it already changes the temperature by the water it holds").defineInRange("drinkCooling", 0.0, 0.0, 100.0);
        COLD_SWEAT_DRINK_COOLING_TICKS = BUILDER.comment("0: drinkCooling lowers the body temperature once, which then drifts back with the surroundings;",
                        "more: it lowers the base temperature for this many ticks instead, like Cold Sweat's cold foods").defineInRange("drinkCoolingTicks", 0, 0, 72000);
        BUILDER.pop();

        BUILDER.comment("Serene Seasons, only without Cold Sweat (its body temperature already follows the seasons)").push("sereneseasons");
        SERENE_SEASONS_ENABLED = BUILDER.comment("Whether the biome climate formula uses the biome temperature as the season changes it (Serene Seasons' biome_temp_adjustment per sub-season)").define("enabled", true);
        SERENE_SEASONS_TROPICAL_DRY = BUILDER.comment("With enabled: multiplies thirst loss in a tropical biome's dry season; the temperature is not changed").defineInRange("tropicalDrySeasonMultiplier", 1.1, 0.0, 10.0);
        SERENE_SEASONS_TROPICAL_WET = BUILDER.comment("With enabled: multiplies thirst loss in a tropical biome's wet season; the temperature is not changed").defineInRange("tropicalWetSeasonMultiplier", 1.0, 0.0, 10.0);
        SERENE_SEASONS_SPRING = defineSeason("spring");
        SERENE_SEASONS_SUMMER = defineSeason("summer");
        SERENE_SEASONS_AUTUMN = defineSeason("autumn");
        SERENE_SEASONS_WINTER = defineSeason("winter");
        BUILDER.pop();

        BUILDER.comment("Reliquary: the Emperor's Chalice hydrates like a drink of pure water (values in the blue_droplets:drinks data map); the Infernal Chalice does not").push("reliquary");
        RELIQUARY_EMPEROR_CHALICE_COOLDOWN = BUILDER.comment("Ticks before the Emperor's Chalice can be used again after a drink (20 ticks = 1 second); 0 = no cooldown").defineInRange("emperorChaliceCooldown", 0, 0, 72000);
        BUILDER.pop();

        BUILDER.comment("Farmer's Delight addons and Let's Do mods").push("delight");
        KETTLE_MIN_PURITY = BUILDER.comment("Lowest water purity that kettles take (they boil it): any block in the block tag blue_droplets:rejects_dirty_water",
                        "(on 1.19.2 the tag is empty: the HerbalBrews and Brewery kettles have no 1.19.2 version). Dirtier water stays in the hand. 0 = any water")
                .defineInRange("kettleMinPurity", PurityLevel.MURKY.level(), PurityLevel.MIN, PurityLevel.MAX);
        WORLD_PURITY_WATER_SOURCES = BUILDER.comment("Whether taps, sinks and wells of other mods give water with the purity of the world's water at their position, like water",
                        "taken from a source block there; false = water without a purity, which reads as defaultPurity. On 1.19.2 no supported mod has one (Extra Delight and Farm & Charm have no 1.19.2 version)").define("worldPurityWaterSources", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private CompatConfig() {}

    private static ForgeConfigSpec.DoubleValue defineSeason(String season)
    {
        return BUILDER.comment("With enabled: multiplies thirst loss in " + season + " in biomes with the four seasons; the temperature is not changed (1 = no change)")
                .defineInRange(season + "Multiplier", 1.0, 0.0, 10.0);
    }
}
