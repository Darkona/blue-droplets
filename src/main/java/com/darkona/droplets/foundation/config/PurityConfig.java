package com.darkona.droplets.foundation.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * {@code config/bluedroplets/purity.toml}: water purity in the world and its effects.
 */
public final class PurityConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.IntValue DEFAULT_PURITY;
    public static final ModConfigSpec.BooleanValue QUENCH_WHEN_DEBUFFED;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALTITUDE_BANDS;
    public static final ModConfigSpec.BooleanValue ALTITUDE_RELATIVE_TO_SEA_LEVEL;
    public static final ModConfigSpec.IntValue WORLD_WATER_BASE_PURITY;
    public static final ModConfigSpec.IntValue SALT_WATER_PURITY;
    public static final ModConfigSpec.IntValue RUNNING_WATER_PURIFICATION_AMOUNT;
    public static final ModConfigSpec.IntValue STILL_WATER_PURIFICATION_AMOUNT;
    public static final ModConfigSpec.IntValue RAIN_CAULDRON_PURITY;
    public static final ModConfigSpec.IntValue DRIPSTONE_CAULDRON_PURITY;
    public static final ModConfigSpec.BooleanValue CAULDRON_BOILING;
    public static final ModConfigSpec.IntValue CAULDRON_BOILING_MAX_PURITY;
    public static final ModConfigSpec.DoubleValue CAULDRON_BOILING_CHANCE;

    /** Keys of the effect lists in {@code [effects]}, by purity 0-3. */
    public static final String[] EFFECT_LEVELS = {"dirty", "slightlyDirty", "acceptable", "purified"};
    /** Effect lists by purity 0-3. */
    public static final List<ModConfigSpec.ConfigValue<List<? extends String>>> EFFECTS;

    public static final ModConfigSpec.BooleanValue HOT_DIRTY_WATER;
    public static final ModConfigSpec.IntValue HOT_DIRTY_WATER_MAX_PURITY;
    public static final ModConfigSpec.IntValue HOT_DIRTY_WATER_DURATION;
    public static final ModConfigSpec.IntValue HOT_DIRTY_WATER_AMPLIFIER;
    public static final ModConfigSpec.DoubleValue HOT_DIRTY_WATER_MIN_BIOME_TEMPERATURE;
    public static final ModConfigSpec.BooleanValue HOT_DIRTY_WATER_COLD_SWEAT;
    public static final ModConfigSpec.DoubleValue HOT_DIRTY_WATER_COLD_SWEAT_MIN_BODY_TEMP;

    public static final ModConfigSpec SPEC;

    static
    {
        BUILDER.push("general");
        ENABLED = BUILDER.comment("Whether water has a purity at all. When false no purity is stored or shown, drinking never gives purity effects",
                "and the purification recipes are not loaded (after /reload or a restart)").define("enabled", true);
        DEFAULT_PURITY = BUILDER.comment("Purity of water that has none stored (0 dirty, 1 slightly dirty, 2 acceptable, 3 purified)").defineInRange("defaultPurity", 2, 0, 3);
        QUENCH_WHEN_DEBUFFED = BUILDER.comment("Whether drinking still restores thirst when a purity effect blocks hydration").define("quenchWhenDebuffed", true);
        BUILDER.pop();

        BUILDER.push("world");
        ALTITUDE_BANDS = BUILDER.comment("Purity added to water by height: [\"minY,maxY,delta\", ...], both ends included; the first band that matches is used.",
                        "Default: +1 at 38 or more blocks above sea level (mountains) and at 16 or more below it (caves)")
                .<String>defineListAllowEmpty("altitudeBands", List.of("38,4096,1", "-4096,-16,1"), () -> "0,0,0", PurityConfig::isValidAltitudeBand);
        ALTITUDE_RELATIVE_TO_SEA_LEVEL = BUILDER.comment("Whether altitudeBands are measured from the dimension's sea level (true) or are absolute Y levels (false)").define("altitudeRelativeToSeaLevel", true);
        WORLD_WATER_BASE_PURITY = BUILDER.comment("Base purity of water in the world when neither its biome (bluedroplets:water_purity/N tags, bluedroplets:biome_water data map)",
                "nor its dimension type (bluedroplets:dimension_water data map) sets one").defineInRange("worldWaterBasePurity", 0, 0, 3);
        SALT_WATER_PURITY = BUILDER.comment("Fixed purity of water in biomes tagged bluedroplets:salt_water (oceans by default); -1 treats it like any other water").defineInRange("saltWaterPurity", -1, -1, 3);
        RUNNING_WATER_PURIFICATION_AMOUNT = BUILDER.comment("Purity added to flowing water").defineInRange("runningWaterPurificationAmount", 1, 0, 3);
        STILL_WATER_PURIFICATION_AMOUNT = BUILDER.comment("Purity added to still (source) water; negative values make it dirtier").defineInRange("stillWaterPurificationAmount", 0, -3, 3);
        RAIN_CAULDRON_PURITY = BUILDER.comment("Purity of rain water collected in a cauldron (mixed with water already there: the lower purity wins); -1 leaves it unset, which reads as defaultPurity").defineInRange("rainCauldronPurity", -1, -1, 3);
        DRIPSTONE_CAULDRON_PURITY = BUILDER.comment("Purity of water dripping from pointed dripstone into a cauldron (mixed as above); -1 leaves it unset, which reads as defaultPurity").defineInRange("dripstoneCauldronPurity", -1, -1, 3);
        BUILDER.pop();

        BUILDER.push("cauldron");
        CAULDRON_BOILING = BUILDER.comment("Whether a water cauldron on a heat source (block tag bluedroplets:cauldron_heat_sources; blocks with a lit property only when lit) slowly gains purity").define("boiling", false);
        CAULDRON_BOILING_MAX_PURITY = BUILDER.comment("Purity boiling stops at").defineInRange("boilingMaxPurity", 3, 0, 3);
        CAULDRON_BOILING_CHANCE = BUILDER.comment("Chance of +1 purity on each random tick of the cauldron. At the default randomTickSpeed (3) a block gets a random tick about every 68 seconds,",
                "so 0.25 is about +1 purity every 4.5 minutes").defineInRange("boilingChance", 0.25, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Effects of drinking water (or a drink with a purity) of each purity: [\"effect_id,durationTicks,amplifier,chancePercent[,blocksHydration]\", ...].",
                "One roll per drink is shared by all entries of a list: an entry applies when the roll is below its chance, so a 30% entry",
                "always comes together with the 100% ones. blocksHydration (default false): the drink restores no thirst unless quenchWhenDebuffed")
                .push("effects");
        EFFECTS = List.of(
                effects(0, List.of("minecraft:nausea,100,0,100", "minecraft:hunger,600,0,100", "minecraft:poison,200,0,30,true")),
                effects(1, List.of("minecraft:nausea,100,0,50", "minecraft:hunger,600,0,50", "minecraft:poison,200,0,10,true")),
                effects(2, List.of("minecraft:nausea,100,0,5", "minecraft:hunger,600,0,5")),
                effects(3, List.of()));
        BUILDER.pop();

        BUILDER.comment("Drinking water of low purity in a hot climate also gives Dehydration (added to the effects above, so PurityEffectEvent sees it).",
                "Hot: an ultra-warm dimension (Nether), a biome temperature at or above minBiomeTemperature (desert, savanna, badlands: 2.0; jungle: 0.95),",
                "or, with Cold Sweat, a body temperature above coldSweatMinBodyTemp")
                .push("hotDirtyWater");
        HOT_DIRTY_WATER = BUILDER.define("enabled", true);
        HOT_DIRTY_WATER_MAX_PURITY = BUILDER.comment("Highest purity that counts (0 dirty, 1 slightly dirty, 2 acceptable, 3 purified)").defineInRange("maxPurity", 0, 0, 3);
        HOT_DIRTY_WATER_DURATION = BUILDER.comment("Duration of the Dehydration effect, in ticks").defineInRange("durationTicks", 600, 1, 1_000_000);
        HOT_DIRTY_WATER_AMPLIFIER = BUILDER.comment("Amplifier of the Dehydration effect (0 is level I)").defineInRange("amplifier", 0, 0, 255);
        HOT_DIRTY_WATER_MIN_BIOME_TEMPERATURE = BUILDER.comment("Biome base temperature from which the climate is hot").defineInRange("minBiomeTemperature", 1.0, -2.0, 5.0);
        HOT_DIRTY_WATER_COLD_SWEAT = BUILDER.comment("Whether Cold Sweat's body temperature also counts as hot (when Cold Sweat is installed)").define("useColdSweat", true);
        HOT_DIRTY_WATER_COLD_SWEAT_MIN_BODY_TEMP = BUILDER.comment("Cold Sweat body temperature (its own units: 0 neutral, 100 burning, -100 freezing) above which the player counts as hot")
                .defineInRange("coldSweatMinBodyTemp", 50.0, -150.0, 150.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private PurityConfig() {}

    private static ModConfigSpec.ConfigValue<List<? extends String>> effects(int purity, List<String> defaults)
    {
        return BUILDER.<String>defineListAllowEmpty(EFFECT_LEVELS[purity], defaults, () -> "minecraft:nausea,100,0,100", PurityConfig::isValidEffect);
    }

    /**
     * {@code "namespace:effect,durationTicks 1+,amplifier 0-255,chancePercent 0-100[,true|false]"}; the effect id is
     * checked later, when registries are ready.
     */
    public static boolean isValidEffect(Object entry)
    {
        if (!(entry instanceof String effect))
            return false;
        String[] parts = effect.split(",");
        if (parts.length < 4 || parts.length > 5 || ResourceLocation.tryParse(parts[0].trim()) == null)
            return false;
        if (parts.length == 5 && !parts[4].trim().equals("true") && !parts[4].trim().equals("false"))
            return false;
        try
        {
            int duration = Integer.parseInt(parts[1].trim());
            int amplifier = Integer.parseInt(parts[2].trim());
            double chance = Double.parseDouble(parts[3].trim());
            return duration >= 1 && amplifier >= 0 && amplifier <= 255 && chance >= 0 && chance <= 100;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    /**
     * {@code "minY,maxY,delta"} with minY <= maxY and delta -3..3.
     */
    public static boolean isValidAltitudeBand(Object entry)
    {
        if (!(entry instanceof String band))
            return false;
        String[] parts = band.split(",");
        if (parts.length != 3)
            return false;
        try
        {
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            int delta = Integer.parseInt(parts[2].trim());
            return min <= max && delta >= -3 && delta <= 3;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }
}
