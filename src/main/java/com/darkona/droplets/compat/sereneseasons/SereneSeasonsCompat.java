package com.darkona.droplets.compat.sereneseasons;

import com.darkona.droplets.foundation.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.fml.ModList;

/**
 * Serene Seasons (compile only, all rights reserved): the biome climate formula reads the biome's temperature as
 * Serene Seasons changes it with the season, so winter lowers thirst loss where it cools the biome; tropical biomes,
 * whose temperature does not change, lose a little more water in their dry season.
 */
public final class SereneSeasonsCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("sereneseasons");

    private SereneSeasonsCompat() {}

    /** The biome's temperature as Serene Seasons changes it with the season, or {@code base}. Serene Seasons must be loaded. */
    public static float temperature(Level level, Holder<Biome> biome, BlockPos pos, float base)
    {
        return CompatConfig.SERENE_SEASONS_ENABLED.get() ? SereneSeasonsBridge.temperature(level, biome, pos) : base;
    }

    /** The {@code sereneseasons} multiplier of the biome's current season (spring to winter, tropical dry or wet), else 1. Serene Seasons must be loaded. */
    public static float seasonMultiplier(Level level, Holder<Biome> biome)
    {
        if (!CompatConfig.SERENE_SEASONS_ENABLED.get())
            return 1.0F;
        return switch (SereneSeasonsBridge.season(level, biome))
        {
            case 0 -> CompatConfig.SERENE_SEASONS_SPRING.get().floatValue();
            case 1 -> CompatConfig.SERENE_SEASONS_SUMMER.get().floatValue();
            case 2 -> CompatConfig.SERENE_SEASONS_AUTUMN.get().floatValue();
            case 3 -> CompatConfig.SERENE_SEASONS_WINTER.get().floatValue();
            case SereneSeasonsBridge.TROPICAL_DRY -> CompatConfig.SERENE_SEASONS_TROPICAL_DRY.get().floatValue();
            case SereneSeasonsBridge.TROPICAL_WET -> CompatConfig.SERENE_SEASONS_TROPICAL_WET.get().floatValue();
            default -> 1.0F;
        };
    }
}
