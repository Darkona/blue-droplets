package com.darkona.droplets.foundation.config;

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

    public static final ModConfigSpec.IntValue DIRTY_POISON_PERCENTAGE;
    public static final ModConfigSpec.IntValue DIRTY_NAUSEA_PERCENTAGE;
    public static final ModConfigSpec.IntValue SLIGHTLY_DIRTY_POISON_PERCENTAGE;
    public static final ModConfigSpec.IntValue SLIGHTLY_DIRTY_NAUSEA_PERCENTAGE;
    public static final ModConfigSpec.IntValue ACCEPTABLE_POISON_PERCENTAGE;
    public static final ModConfigSpec.IntValue ACCEPTABLE_NAUSEA_PERCENTAGE;
    public static final ModConfigSpec.IntValue PURIFIED_POISON_PERCENTAGE;
    public static final ModConfigSpec.IntValue PURIFIED_NAUSEA_PERCENTAGE;

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

        BUILDER.push("effects");
        DIRTY_POISON_PERCENTAGE = BUILDER.comment("% of getting poisoned after drinking dirty water").defineInRange("dirtyPoisonPercentage", 30, 0, 100);
        DIRTY_NAUSEA_PERCENTAGE = BUILDER.comment("% of getting sick (hunger and nausea) after drinking dirty water").defineInRange("dirtyNauseaPercentage", 100, 0, 100);
        SLIGHTLY_DIRTY_POISON_PERCENTAGE = BUILDER.comment("% of getting poisoned after drinking slightly dirty water").defineInRange("slightlyDirtyPoisonPercentage", 10, 0, 100);
        SLIGHTLY_DIRTY_NAUSEA_PERCENTAGE = BUILDER.comment("% of getting sick (hunger and nausea) after drinking slightly dirty water").defineInRange("slightlyDirtyNauseaPercentage", 50, 0, 100);
        ACCEPTABLE_POISON_PERCENTAGE = BUILDER.comment("% of getting poisoned after drinking acceptable water").defineInRange("acceptablePoisonPercentage", 0, 0, 100);
        ACCEPTABLE_NAUSEA_PERCENTAGE = BUILDER.comment("% of getting sick (hunger and nausea) after drinking acceptable water").defineInRange("acceptableNauseaPercentage", 5, 0, 100);
        PURIFIED_POISON_PERCENTAGE = BUILDER.comment("% of getting poisoned after drinking purified water").defineInRange("purifiedPoisonPercentage", 0, 0, 100);
        PURIFIED_NAUSEA_PERCENTAGE = BUILDER.comment("% of getting sick (hunger and nausea) after drinking purified water").defineInRange("purifiedNauseaPercentage", 0, 0, 100);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private PurityConfig() {}

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
