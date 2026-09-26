package com.darkona.dropletsofthirst.compat.sereneseasons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.init.ModConfig;
import sereneseasons.init.ModTags;
import sereneseasons.season.SeasonHooks;

/**
 * The only class that touches Serene Seasons. Loaded only through {@link SereneSeasonsCompat} when it is present.
 * Its API has the season but not the seasonal temperature, so that comes from {@code SeasonHooks}, the method Serene
 * Seasons itself uses for snow and rain.
 */
final class SereneSeasonsBridge
{
    private SereneSeasonsBridge() {}

    /**
     * The biome's temperature at {@code pos} in the current sub-season: its own plus Serene Seasons'
     * {@code biome_temp_adjustment}, except where the season does not apply (tropical, warm or blacklisted biomes,
     * other dimensions).
     */
    static float temperature(Level level, Holder<Biome> biome, BlockPos pos)
    {
        return SeasonHooks.getBiomeTemperature(level, biome, pos, level.getSeaLevel());
    }

    /** No seasons here. */
    static final int NONE = -1;
    /** Tropical dry and wet season; 0-3 are spring, summer, autumn and winter. */
    static final int TROPICAL_DRY = 4, TROPICAL_WET = 5;

    /** The season of {@code biome}: {@link #NONE}, 0-3 (spring to winter), or a tropical one. */
    static int season(Level level, Holder<Biome> biome)
    {
        if (biome.is(ModTags.Biomes.BLACKLISTED_BIOMES) || !ModConfig.seasons.isDimensionWhitelisted(level.dimension()))
            return NONE;
        if (SeasonHelper.usesTropicalSeasons(biome))
            return SeasonHelper.getSeasonState(level).getTropicalSeason().ordinal() < 3 ? TROPICAL_DRY : TROPICAL_WET;
        return SeasonHelper.getSeasonState(level).getSeason().ordinal();
    }
}
