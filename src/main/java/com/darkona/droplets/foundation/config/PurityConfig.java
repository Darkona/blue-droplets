package com.darkona.droplets.foundation.config;

import com.darkona.droplets.api.PurityLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

/**
 * {@code config/blue_droplets/purity.toml}: water purity in the world and its effects.
 */
public final class PurityConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue DEFAULT_PURITY;
    public static final ForgeConfigSpec.BooleanValue QUENCH_WHEN_DEBUFFED;
    public static final ForgeConfigSpec.IntValue PURE_THIRST_BONUS;
    public static final ForgeConfigSpec.IntValue PURE_QUENCHED_BONUS;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALTITUDE_BANDS;
    public static final ForgeConfigSpec.BooleanValue ALTITUDE_RELATIVE_TO_SEA_LEVEL;
    public static final ForgeConfigSpec.IntValue WORLD_WATER_BASE_PURITY;
    public static final ForgeConfigSpec.IntValue SALT_WATER_PURITY;
    public static final ForgeConfigSpec.IntValue RUNNING_WATER_PURIFICATION_AMOUNT;
    public static final ForgeConfigSpec.IntValue STILL_WATER_PURIFICATION_AMOUNT;

    /** Keys of the effect lists in {@code [effects]}, by purity: the {@link PurityLevel} ids. */
    public static final String[] EFFECT_LEVELS = Arrays.stream(PurityLevel.values()).map(PurityLevel::id).toArray(String[]::new);
    /** Effect lists by purity. */
    public static final List<ForgeConfigSpec.ConfigValue<List<? extends String>>> EFFECTS;

    public static final ForgeConfigSpec.BooleanValue HOT_DIRTY_WATER;
    public static final ForgeConfigSpec.IntValue HOT_DIRTY_WATER_MAX_PURITY;
    public static final ForgeConfigSpec.IntValue HOT_DIRTY_WATER_DURATION;
    public static final ForgeConfigSpec.IntValue HOT_DIRTY_WATER_AMPLIFIER;
    public static final ForgeConfigSpec.DoubleValue HOT_DIRTY_WATER_MIN_BIOME_TEMPERATURE;
    public static final ForgeConfigSpec.BooleanValue HOT_DIRTY_WATER_COLD_SWEAT;
    public static final ForgeConfigSpec.DoubleValue HOT_DIRTY_WATER_COLD_SWEAT_MIN_BODY_TEMP;

    public static final ForgeConfigSpec SPEC;

    /** "0 contaminated, 1 dirty, ..." for config comments. */
    private static final String LEVELS = String.join(", ", Arrays.stream(PurityLevel.values()).map(level -> level.level() + " " + level.id()).toList());

    static
    {
        BUILDER.push("general");
        ENABLED = BUILDER.comment("Whether water has a purity at all. When false no purity is stored or shown, drinking never gives purity effects",
                "and the purification recipes are not loaded (after /reload or a restart)").define("enabled", true);
        DEFAULT_PURITY = BUILDER.comment("Purity of water that has none stored (" + LEVELS + ")").defineInRange("defaultPurity", PurityLevel.ACCEPTABLE.level(), PurityLevel.MIN, PurityLevel.MAX);
        QUENCH_WHEN_DEBUFFED = BUILDER.comment("Whether drinking still restores thirst when a purity effect blocks hydration").define("quenchWhenDebuffed", true);
        BUILDER.pop();

        BUILDER.comment("Pure water (purity " + PurityLevel.PURE.level() + ") hydrates more than other water: these points are added to what the container gives.",
                "Bottles, buckets, bowls, the Traveler's Backpack hose, Cold Sweat waterskins and other purity containers count; a sip by hand does not.",
                "Other drinks with a purity do not get it. 2 points = 1 droplet on the HUD").push("pureWater");
        PURE_THIRST_BONUS = BUILDER.comment("Thirst added when drinking pure water").defineInRange("thirstBonus", 2, 0, 20);
        PURE_QUENCHED_BONUS = BUILDER.comment("Quenched added when drinking pure water").defineInRange("quenchedBonus", 3, 0, 20);
        BUILDER.pop();

        BUILDER.push("world");
        ALTITUDE_BANDS = BUILDER.comment("Purity added to water by height: [\"minY,maxY,delta\", ...], both ends included; the first band that matches is used.",
                        "Default, from sea level: +1 from 30 blocks above it, +2 from 60, +3 from 100 (mountains); +1 from 16 blocks below it, +2 from 48, +3 from 80 (caves)")
                .<String>defineListAllowEmpty("altitudeBands", List.of("30,59,1", "60,99,2", "100,4096,3", "-47,-16,1", "-79,-48,2", "-4096,-80,3"),
                        PurityConfig::isValidAltitudeBand);
        ALTITUDE_RELATIVE_TO_SEA_LEVEL = BUILDER.comment("Whether altitudeBands are measured from the dimension's sea level (true) or are absolute Y levels (false)").define("altitudeRelativeToSeaLevel", true);
        WORLD_WATER_BASE_PURITY = BUILDER.comment("Base purity of water in the world when neither its biome (blue_droplets:water_purity/N tags, blue_droplets:biome_water data map)",
                "nor its dimension type (blue_droplets:dimension_water data map) sets one").defineInRange("worldWaterBasePurity", PurityLevel.DIRTY.level(), PurityLevel.MIN, PurityLevel.MAX);
        SALT_WATER_PURITY = BUILDER.comment("Fixed purity of water in biomes tagged blue_droplets:salt_water (oceans by default); -1 treats it like any other water")
                .defineInRange("saltWaterPurity", PurityLevel.CONTAMINATED.level(), -1, PurityLevel.MAX);
        RUNNING_WATER_PURIFICATION_AMOUNT = BUILDER.comment("Purity added to flowing water").defineInRange("runningWaterPurificationAmount", 1, 0, PurityLevel.MAX);
        STILL_WATER_PURIFICATION_AMOUNT = BUILDER.comment("Purity added to still (source) water; negative values make it dirtier").defineInRange("stillWaterPurificationAmount", 0, -PurityLevel.MAX, PurityLevel.MAX);
        BUILDER.pop();

        BUILDER.comment("Effects of drinking water (or a drink with a purity) of each purity: [\"effect_id,durationTicks,amplifier,chancePercent[,blocksHydration]\", ...].",
                "One roll per drink is shared by all entries of a list: an entry applies when the roll is below its chance, so a 30% entry",
                "always comes together with the 100% ones. blocksHydration (default false): the drink restores no thirst unless quenchWhenDebuffed")
                .push("effects");
        EFFECTS = List.of(
                effects(PurityLevel.CONTAMINATED, List.of("minecraft:nausea,100,0,100", "minecraft:hunger,600,0,100", "minecraft:poison,200,0,40,true")),
                effects(PurityLevel.DIRTY, List.of("minecraft:nausea,100,0,60", "minecraft:hunger,600,0,60", "minecraft:poison,200,0,15,true")),
                effects(PurityLevel.MURKY, List.of("minecraft:nausea,100,0,25", "minecraft:hunger,600,0,25")),
                effects(PurityLevel.ACCEPTABLE, List.of("minecraft:nausea,100,0,5", "minecraft:hunger,600,0,5")),
                effects(PurityLevel.CLEAN, List.of()),
                effects(PurityLevel.PURE, List.of()));
        BUILDER.pop();

        BUILDER.comment("Drinking water of low purity in a hot climate also gives Dehydration (added to the effects above, so PurityEffectEvent sees it).",
                "Hot: an ultra-warm dimension (Nether), a biome temperature at or above minBiomeTemperature (desert, savanna, badlands: 2.0; jungle: 0.95),",
                "or, with Cold Sweat, a body temperature above coldSweatMinBodyTemp")
                .push("hotDirtyWater");
        HOT_DIRTY_WATER = BUILDER.define("enabled", true);
        HOT_DIRTY_WATER_MAX_PURITY = BUILDER.comment("Highest purity that counts (" + LEVELS + ")").defineInRange("maxPurity", PurityLevel.MURKY.level(), PurityLevel.MIN, PurityLevel.MAX);
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

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> effects(PurityLevel level, List<String> defaults)
    {
        return BUILDER.<String>defineListAllowEmpty(level.id(), defaults, PurityConfig::isValidEffect);
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
     * {@code "minY,maxY,delta"} with minY <= maxY and delta between -{@link PurityLevel#MAX} and {@link PurityLevel#MAX}.
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
            return min <= max && delta >= -PurityLevel.MAX && delta <= PurityLevel.MAX;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }
}
