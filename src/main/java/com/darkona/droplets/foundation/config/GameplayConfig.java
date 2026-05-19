package com.darkona.droplets.foundation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code config/bluedroplets/gameplay.toml}: thirst loss, drinking, damage and regeneration rules.
 */
public final class GameplayConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue DEPLETION_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue DEPLETES_IN_PEACEFUL;
    public static final ModConfigSpec.DoubleValue NETHER_MULTIPLIER;
    public static final ModConfigSpec.IntValue FIRE_RESISTANCE_PERCENT;
    public static final ModConfigSpec.BooleanValue DEPLETES_WHEN_NAUSEOUS;

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
        DEPLETION_MULTIPLIER = BUILDER.comment("How much faster thirst goes down than hunger (1 = same speed). Was thirstDepletionModifier")
                .defineInRange("multiplier", 1.2, 0.0, 10.0);
        DEPLETES_IN_PEACEFUL = BUILDER.comment("Whether thirst goes down in Peaceful").define("inPeaceful", false);
        NETHER_MULTIPLIER = BUILDER.comment("Replaces the climate multiplier in ultra-warm dimensions (the Nether)").defineInRange("netherMultiplier", 3.0, 1.0, 5.0);
        FIRE_RESISTANCE_PERCENT = BUILDER.comment("Thirst loss with Fire Resistance, in percent (0 = none, 100 = normal)").defineInRange("fireResistancePercent", 0, 0, 100);
        DEPLETES_WHEN_NAUSEOUS = BUILDER.comment("Whether Nausea makes thirst go down").define("nauseaDepletes", true);
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
}
