package com.darkona.droplets.foundation.config;

import com.darkona.droplets.api.PurityLevel;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * {@code config/blue_droplets/compat.toml}: one section per optional mod; ignored when that mod is not installed.
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

    public static final ModConfigSpec.BooleanValue SERENE_SEASONS_ENABLED;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_TROPICAL_DRY;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_TROPICAL_WET;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_SPRING;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_SUMMER;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_AUTUMN;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_WINTER;

    public static final ModConfigSpec.IntValue RELIQUARY_EMPEROR_CHALICE_COOLDOWN;

    public static final ModConfigSpec.DoubleValue VAMPIRISM_THIRST_PER_BLOOD;
    public static final ModConfigSpec.DoubleValue VAMPIRISM_QUENCHED_PER_BLOOD;

    public static final ModConfigSpec.IntValue KETTLE_MIN_PURITY;
    public static final ModConfigSpec.BooleanValue WORLD_PURITY_WATER_SOURCES;

    public static final ModConfigSpec SPEC;

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
                .<String>defineListAllowEmpty("bodyTemperatureCurve", List.of("-100,0.8", "0,1.0", "50,1.3", "100,2.0", "150,3.0"), () -> "0,1.0", GameplayConfig::isValidPair);
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

        BUILDER.comment("Vampirism: a vampire's thirst drops like anyone's and only blood hydrates it. Every drop of blood it drinks (bites, blood bottles,",
                "blood containers, blood food) also restores thirst; the altars and /vampirism commands that fill the blood bar do not").push("vampirism");
        VAMPIRISM_THIRST_PER_BLOOD = BUILDER.comment("Thirst points per point of blood drunk; the blood bar and the thirst bar are both 20, so 1.0 = a full blood refill fills the thirst bar")
                .defineInRange("thirstPerBlood", 1.0, 0.0, 20.0);
        VAMPIRISM_QUENCHED_PER_BLOOD = BUILDER.comment("Quenched per point of blood drunk, times the blood's saturation in Vampirism (0.3 poor to 1.0 rich; a blood bottle is 0.45)")
                .defineInRange("quenchedPerBlood", 1.0, 0.0, 20.0);
        BUILDER.pop();

        BUILDER.comment("Farmer's Delight addons and Let's Do mods").push("delight");
        KETTLE_MIN_PURITY = BUILDER.comment("Lowest water purity that kettles take (they boil it): the HerbalBrews tea kettle, the Brewery brewing stations",
                        "and any block in the block tag blue_droplets:rejects_dirty_water. Dirtier water stays in the slot or in the hand. 0 = any water")
                .defineInRange("kettleMinPurity", PurityLevel.MURKY.level(), PurityLevel.MIN, PurityLevel.MAX);
        WORLD_PURITY_WATER_SOURCES = BUILDER.comment("Whether taps and sinks (Extra Delight, Farm & Charm) and the Farm & Charm timber well give water with the purity of the world's water",
                        "at their position, like water taken from a source block there; false = water without a purity, which reads as defaultPurity").define("worldPurityWaterSources", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private CompatConfig() {}

    private static ModConfigSpec.DoubleValue defineSeason(String season)
    {
        return BUILDER.comment("With enabled: multiplies thirst loss in " + season + " in biomes with the four seasons; the temperature is not changed (1 = no change)")
                .defineInRange(season + "Multiplier", 1.0, 0.0, 10.0);
    }
}
