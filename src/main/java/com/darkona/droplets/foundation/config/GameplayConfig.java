package com.darkona.droplets.foundation.config;

import com.darkona.droplets.core.NumberRows;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * {@code config/bluedroplets/gameplay.toml}: thirst loss, drinking, damage and regeneration rules.
 */
public final class GameplayConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public enum Mode { MIRROR_FOOD, OWN }

    public enum ClimateFormula { LEGACY, CURVE }

    public static final ModConfigSpec.EnumValue<Mode> MODE;
    public static final ModConfigSpec.DoubleValue BASAL_PER_TICK;
    public static final ModConfigSpec.DoubleValue DEPLETION_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue DEPLETES_IN_PEACEFUL;
    public static final ModConfigSpec.DoubleValue NETHER_MULTIPLIER;
    public static final ModConfigSpec.IntValue FIRE_RESISTANCE_PERCENT;
    public static final ModConfigSpec.BooleanValue DEPLETES_WHEN_NAUSEOUS;
    public static final ModConfigSpec.DoubleValue FIRE_PROTECTION_PER_LEVEL;
    public static final ModConfigSpec.IntValue FIRE_PROTECTION_MAX_LEVELS;

    public static final ModConfigSpec.EnumValue<ClimateFormula> CLIMATE_FORMULA;
    public static final ModConfigSpec.DoubleValue LEGACY_HARSHNESS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> TEMPERATURE_CURVE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HUMIDITY_CURVE;
    public static final ModConfigSpec.DoubleValue RAIN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue THUNDER_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue DAY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue NIGHT_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SUN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue IN_WATER_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue UNDERWATER_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALTITUDE_MULTIPLIERS;

    public static final ModConfigSpec.DoubleValue SPRINT_PER_METER;
    public static final ModConfigSpec.DoubleValue SWIM_PER_METER;
    public static final ModConfigSpec.DoubleValue JUMP;
    public static final ModConfigSpec.DoubleValue SPRINT_JUMP;
    public static final ModConfigSpec.DoubleValue ATTACK;
    public static final ModConfigSpec.DoubleValue BLOCK_BREAK;
    public static final ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HEAL_PER_HEALTH;
    public static final ModConfigSpec.DoubleValue RIDING_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SLEEPING_MULTIPLIER;

    public static final ModConfigSpec.BooleanValue REGEN_HALTED_WHEN_THIRSTY;
    public static final ModConfigSpec.BooleanValue REGEN_DEPLETES_THIRST;
    public static final ModConfigSpec.BooleanValue REGEN_CLIMATE_DEPENDENT;

    public static final ModConfigSpec.BooleanValue SPRINT_BLOCKED_WHEN_THIRSTY;

    public static final ModConfigSpec.BooleanValue EXTRA_THIRST_TO_QUENCHED;
    public static final ModConfigSpec.IntValue WATER_BOTTLE_STACK_SIZE;
    public static final ModConfigSpec.BooleanValue RAIN_DRINKING;
    public static final ModConfigSpec.BooleanValue HAND_DRINKING;
    public static final ModConfigSpec.BooleanValue HAND_DRINKING_BOTH_HANDS;
    public static final ModConfigSpec.IntValue HAND_DRINKING_THIRST;
    public static final ModConfigSpec.IntValue HAND_DRINKING_QUENCHED;
    public static final ModConfigSpec.IntValue HAND_DRINKING_COOLDOWN;

    public static final ModConfigSpec.BooleanValue LOOT;

    public static final ModConfigSpec SPEC;

    static
    {
        BUILDER.push("depletion");
        MODE = BUILDER.comment("MIRROR_FOOD: thirst follows the exhaustion vanilla adds to hunger (as before; includes other mods' exhaustion).",
                "OWN: Blue Droplets counts the activities in [depletion.activity] itself, with vanilla's numbers by default; hunger is not read")
                .defineEnum("mode", Mode.MIRROR_FOOD);
        BASAL_PER_TICK = BUILDER.comment("Exhaustion added every tick even when doing nothing (4.0 = one quenched or thirst point); scaled by the multipliers")
                .defineInRange("basalPerTick", 0.0, 0.0, 1.0);
        DEPLETION_MULTIPLIER = BUILDER.comment("How much faster thirst goes down than hunger (1 = same speed). Was thirstDepletionModifier")
                .defineInRange("multiplier", 1.2, 0.0, 10.0);
        DEPLETES_IN_PEACEFUL = BUILDER.comment("Whether thirst goes down in Peaceful").define("inPeaceful", false);
        NETHER_MULTIPLIER = BUILDER.comment("Replaces the climate multiplier in ultra-warm dimensions (the Nether)").defineInRange("netherMultiplier", 3.0, 1.0, 5.0);
        FIRE_RESISTANCE_PERCENT = BUILDER.comment("Thirst loss with Fire Resistance, in percent (0 = none, 100 = normal)").defineInRange("fireResistancePercent", 0, 0, 100);
        DEPLETES_WHEN_NAUSEOUS = BUILDER.comment("Whether Nausea makes thirst go down").define("nauseaDepletes", true);
        FIRE_PROTECTION_PER_LEVEL = BUILDER.comment("Thirst loss removed per level of Fire Protection on armor (0.046875 = 4.7%)").defineInRange("fireProtectionPerLevel", 0.046875, 0.0, 1.0);
        FIRE_PROTECTION_MAX_LEVELS = BUILDER.comment("Fire Protection levels counted at most").defineInRange("fireProtectionMaxLevels", 12, 0, 100);

        BUILDER.comment("Multipliers from the player's surroundings, recomputed every second. 1.0 = no change").push("climate");
        CLIMATE_FORMULA = BUILDER.comment("LEGACY: 'multiplier' x biome temperature / humidity, softened below 1 by legacyHarshness (as before).",
                        "CURVE: 'multiplier' x temperatureCurve(biome temperature) x humidityCurve(biome downfall).",
                        "With Cold Sweat the body temperature / 100 replaces the biome temperature in both")
                .defineEnum("formula", ClimateFormula.LEGACY);
        LEGACY_HARSHNESS = BUILDER.comment("LEGACY: how much of a multiplier below 1 is kept (0.5 = halfway to 1)").defineInRange("legacyHarshness", 0.5, 0.0, 1.0);
        TEMPERATURE_CURVE = BUILDER.comment("CURVE: [\"temperature,multiplier\", ...] in ascending temperature; straight lines between points, flat beyond the ends")
                .<String>defineListAllowEmpty("temperatureCurve", List.of("-0.5,0.7", "0.8,1.0", "2.0,1.5"), () -> "0.8,1.0", GameplayConfig::isValidPair);
        HUMIDITY_CURVE = BUILDER.comment("CURVE: [\"downfall,multiplier\", ...] in ascending downfall (0 dry to 1 wet)")
                .<String>defineListAllowEmpty("humidityCurve", List.of("0.0,1.2", "0.4,1.0", "1.0,0.8"), () -> "0.4,1.0", GameplayConfig::isValidPair);
        RAIN_MULTIPLIER = BUILDER.comment("When rain falls on the player").defineInRange("rain", 1.0, 0.0, 10.0);
        THUNDER_MULTIPLIER = BUILDER.comment("When rain falls on the player during a thunderstorm (replaces rain)").defineInRange("thunder", 1.0, 0.0, 10.0);
        DAY_MULTIPLIER = BUILDER.comment("During the day, in dimensions with a day cycle").defineInRange("day", 1.0, 0.0, 10.0);
        NIGHT_MULTIPLIER = BUILDER.comment("At night, in dimensions with a day cycle").defineInRange("night", 1.0, 0.0, 10.0);
        SUN_MULTIPLIER = BUILDER.comment("In direct sunlight: day, no rain and the sky visible").defineInRange("sun", 1.0, 0.0, 10.0);
        IN_WATER_MULTIPLIER = BUILDER.comment("While in water with the head out").defineInRange("inWater", 1.0, 0.0, 10.0);
        UNDERWATER_MULTIPLIER = BUILDER.comment("While fully underwater").defineInRange("underwater", 1.0, 0.0, 10.0);
        ALTITUDE_MULTIPLIERS = BUILDER.comment("[\"minY,maxY,multiplier\", ...] measured from the dimension's sea level; the first band containing the player applies")
                .<String>defineListAllowEmpty("altitude", List.of(), () -> "64,4096,1.0", GameplayConfig::isValidAltitude);
        BUILDER.pop();

        BUILDER.comment("Exhaustion per activity. In OWN mode these are the sources (defaults = vanilla hunger exhaustion);",
                "in MIRROR_FOOD mode only ridingMultiplier and sleepingMultiplier apply").push("activity");
        SPRINT_PER_METER = BUILDER.comment("OWN: per meter sprinted on the ground").defineInRange("sprintPerMeter", 0.1, 0.0, 10.0);
        SWIM_PER_METER = BUILDER.comment("OWN: per meter swum or walked in water").defineInRange("swimPerMeter", 0.01, 0.0, 10.0);
        JUMP = BUILDER.comment("OWN: per jump").defineInRange("jump", 0.05, 0.0, 10.0);
        SPRINT_JUMP = BUILDER.comment("OWN: per sprinting jump").defineInRange("sprintJump", 0.2, 0.0, 10.0);
        ATTACK = BUILDER.comment("OWN: per attack").defineInRange("attack", 0.1, 0.0, 10.0);
        BLOCK_BREAK = BUILDER.comment("OWN: per block broken").defineInRange("blockBreak", 0.005, 0.0, 10.0);
        DAMAGE_MULTIPLIER = BUILDER.comment("OWN: times the exhaustion of the damage type taken (vanilla: 0.1 for most damage)").defineInRange("damageMultiplier", 1.0, 0.0, 10.0);
        HEAL_PER_HEALTH = BUILDER.comment("OWN: per health point regenerated from food").defineInRange("healPerHealth", 6.0, 0.0, 100.0);
        RIDING_MULTIPLIER = BUILDER.comment("While riding (0 = no thirst loss from activity, as before)").defineInRange("ridingMultiplier", 0.0, 0.0, 10.0);
        SLEEPING_MULTIPLIER = BUILDER.comment("While sleeping").defineInRange("sleepingMultiplier", 1.0, 0.0, 10.0);
        BUILDER.pop();
        BUILDER.pop();

        BUILDER.push("regeneration");
        REGEN_HALTED_WHEN_THIRSTY = BUILDER.comment("Whether health regenerates slower (or not at all) when thirst is not full, like hunger").define("haltedWhenThirsty", true);
        REGEN_DEPLETES_THIRST = BUILDER.comment("Whether regenerating health makes thirst go down, like hunger").define("depletesThirst", true);
        REGEN_CLIMATE_DEPENDENT = BUILDER.comment("Whether thirst lost by regenerating health is scaled by the climate multiplier").define("climateDependent", true);
        BUILDER.pop();

        BUILDER.push("sprint");
        SPRINT_BLOCKED_WHEN_THIRSTY = BUILDER.comment("Whether players can't sprint with 3 droplets or less").define("blockedWhenThirsty", true);
        BUILDER.pop();

        BUILDER.push("drinking");
        EXTRA_THIRST_TO_QUENCHED = BUILDER.comment("Whether thirst restored above full turns into quenched").define("extraThirstToQuenched", true);
        WATER_BOTTLE_STACK_SIZE = BUILDER.comment("Stack size of water bottles").defineInRange("waterBottleStackSize", 64, 1, 99);
        RAIN_DRINKING = BUILDER.comment("Whether players drink rain by looking up").define("rain", true);
        BUILDER.pop();

        BUILDER.push("hand");
        HAND_DRINKING = BUILDER.comment("Whether players can drink water in the world by sneaking and right-clicking with an empty hand").define("enabled", false);
        HAND_DRINKING_BOTH_HANDS = BUILDER.comment("Whether both hands must be empty").define("bothHandsEmpty", true);
        HAND_DRINKING_THIRST = BUILDER.comment("Thirst restored per sip").defineInRange("thirst", 3, 0, 20);
        HAND_DRINKING_QUENCHED = BUILDER.comment("Quenched restored per sip").defineInRange("quenched", 2, 0, 20);
        HAND_DRINKING_COOLDOWN = BUILDER.comment("Minimum ticks between two sips (20 ticks = 1 second)").defineInRange("cooldownTicks", 10, 0, 1200);
        BUILDER.pop();

        BUILDER.push("loot");
        LOOT = BUILDER.comment("Whether drinks are added to vanilla chest loot").define("enabled", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private GameplayConfig() {}

    public static boolean isValidPair(Object entry)
    {
        return NumberRows.parseRow(entry, 2) != null;
    }

    public static boolean isValidAltitude(Object entry)
    {
        double[] row = NumberRows.parseRow(entry, 3);
        return row != null && row[0] <= row[1] && row[2] >= 0;
    }
}
