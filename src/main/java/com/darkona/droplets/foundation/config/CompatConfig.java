package com.darkona.droplets.foundation.config;

import com.darkona.droplets.api.PurityLevel;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code config/blue_droplets/compat.toml}: one section per optional mod; ignored when that mod is not installed.
 */
public final class CompatConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SERENE_SEASONS_ENABLED;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_TROPICAL_DRY;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_TROPICAL_WET;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_SPRING;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_SUMMER;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_AUTUMN;
    public static final ModConfigSpec.DoubleValue SERENE_SEASONS_WINTER;

    public static final ModConfigSpec SPEC;

    static
    {
        BUILDER.comment("Serene Seasons").push("sereneseasons");
        SERENE_SEASONS_ENABLED = BUILDER.comment("Whether the biome climate formula uses the biome temperature as the season changes it (Serene Seasons' biome_temp_adjustment per sub-season)").define("enabled", true);
        SERENE_SEASONS_TROPICAL_DRY = BUILDER.comment("With enabled: multiplies thirst loss in a tropical biome's dry season; the temperature is not changed").defineInRange("tropicalDrySeasonMultiplier", 1.1, 0.0, 10.0);
        SERENE_SEASONS_TROPICAL_WET = BUILDER.comment("With enabled: multiplies thirst loss in a tropical biome's wet season; the temperature is not changed").defineInRange("tropicalWetSeasonMultiplier", 1.0, 0.0, 10.0);
        SERENE_SEASONS_SPRING = defineSeason("spring");
        SERENE_SEASONS_SUMMER = defineSeason("summer");
        SERENE_SEASONS_AUTUMN = defineSeason("autumn");
        SERENE_SEASONS_WINTER = defineSeason("winter");
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
