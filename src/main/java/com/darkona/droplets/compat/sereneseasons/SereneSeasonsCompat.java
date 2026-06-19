package com.darkona.droplets.compat.sereneseasons;

import com.darkona.droplets.compat.coldsweat.ColdSweatCompat;
import com.darkona.droplets.foundation.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.fml.ModList;

/**
 * Serene Seasons (compile only, all rights reserved): the biome climate formula reads the biome's temperature as
 * Serene Seasons changes it with the season, so winter lowers thirst loss where it cools the biome; tropical biomes,
 * whose temperature does not change, lose a little more water in their dry season. Not with Cold Sweat, whose body
 * temperature already follows the seasons.
 */
public final class SereneSeasonsCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("sereneseasons");
    /** Cold Sweat counts the seasons in its temperature: then this compat stays out. */
    public static final boolean ACTIVE = LOADED && !ColdSweatCompat.LOADED;

    private SereneSeasonsCompat() {}

    /** The biome's temperature as Serene Seasons changes it with the season, or {@code base}. Serene Seasons must be loaded. */
    public static float temperature(Level level, Holder<Biome> biome, BlockPos pos, float base)
    {
        return CompatConfig.SERENE_SEASONS_ENABLED.get() ? SereneSeasonsBridge.temperature(level, biome, pos) : base;
    }

    /** {@code sereneseasons.tropicalDrySeasonMultiplier} in a tropical biome's dry season, else 1. Serene Seasons must be loaded. */
    public static float tropicalMultiplier(Level level, Holder<Biome> biome)
    {
        return CompatConfig.SERENE_SEASONS_ENABLED.get() && SereneSeasonsBridge.isDryTropical(level, biome)
                ? CompatConfig.SERENE_SEASONS_TROPICAL_DRY.get().floatValue() : 1.0F;
    }
}
