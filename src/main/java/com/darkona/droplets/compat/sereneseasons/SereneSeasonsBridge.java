package com.darkona.droplets.compat.sereneseasons;

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
        return SeasonHooks.getBiomeTemperature(level, biome, pos);
    }

    /** A tropical biome with seasons, in the dry season (early, mid or late dry). */
    static boolean isDryTropical(Level level, Holder<Biome> biome)
    {
        return SeasonHelper.usesTropicalSeasons(biome) && !biome.is(ModTags.Biomes.BLACKLISTED_BIOMES)
                && ModConfig.seasons.isDimensionWhitelisted(level.dimension())
                && SeasonHelper.getSeasonState(level).getTropicalSeason().ordinal() < 3;
    }
}
