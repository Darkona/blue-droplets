package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.sereneseasons.SereneSeasonsCompat;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.foundation.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import sereneseasons.api.season.Season;
import sereneseasons.season.SeasonHandler;
import sereneseasons.season.SeasonSavedData;
import sereneseasons.season.SeasonTime;

/**
 * Serene Seasons: the biome climate formula with the season's temperature and the tropical dry season. These tests
 * call {@link ThirstHelper#biomeClimate} with the seasons on. Each test sets the season and puts it back in the same
 * call, so no other test sees it.
 * Registered by {@link DropletsGameTests} only when Serene Seasons is installed.
 */
public class SereneSeasonsTests
{
    private static Holder<Biome> biome(ServerLevel level, ResourceKey<Biome> key)
    {
        return level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
    }

    /**
     * Where the tests read the climate: the test's column at sea level. Above sea level + 17 vanilla also cools the
     * biome with height and Serene Seasons' temperature includes that, which the formula without seasons does not;
     * since 26.3 the tests run above the test world's sea level, so they read below it to compare only the season.
     */
    private static BlockPos at(GameTestHelper helper)
    {
        return helper.absolutePos(BlockPos.ZERO).atY(helper.getLevel().getSeaLevel());
    }

    /** Climate multiplier of {@code biome} in the middle of {@code season}, with seasons; the level's season is restored. */
    private static float inSeason(GameTestHelper helper, Holder<Biome> biome, Season.SubSeason season)
    {
        SeasonSavedData data = SeasonHandler.getSeasonSavedData(helper.getLevel());
        int ticks = data.seasonCycleTicks;
        try
        {
            data.seasonCycleTicks = SeasonTime.ZERO.getSubSeasonDuration() * season.ordinal();
            return ThirstHelper.biomeClimate(helper.getLevel(), biome, at(helper), 1.0F, true);
        }
        finally
        {
            data.seasonCycleTicks = ticks;
        }
    }

    private static float withoutSeasons(GameTestHelper helper, Holder<Biome> biome)
    {
        return ThirstHelper.biomeClimate(helper.getLevel(), biome, at(helper), 1.0F, false);
    }

    @GameTest(template = "empty")
    public static void winterLowersThirst(GameTestHelper helper)
    {
        Holder<Biome> plains = biome(helper.getLevel(), Biomes.PLAINS);
        float summer = inSeason(helper, plains, Season.SubSeason.MID_SUMMER);
        float winter = inSeason(helper, plains, Season.SubSeason.MID_WINTER);
        float none = withoutSeasons(helper, plains);
        helper.assertTrue(winter < summer, "plains: winter " + winter + " not below summer " + summer);
        helper.assertTrue(winter < none, "plains: winter " + winter + " not below no seasons " + none);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tropicalDrySeasonRaisesThirst(GameTestHelper helper)
    {
        Holder<Biome> desert = biome(helper.getLevel(), Biomes.DESERT);
        float none = withoutSeasons(helper, desert);
        // Mid summer is the early dry season in the tropics, mid spring the late wet one.
        helper.assertValueEqual(inSeason(helper, desert, Season.SubSeason.MID_SUMMER), CompatConfig.SERENE_SEASONS_TROPICAL_DRY.get().floatValue() * none, "desert, dry season");
        helper.assertValueEqual(inSeason(helper, desert, Season.SubSeason.MID_SPRING), CompatConfig.SERENE_SEASONS_TROPICAL_WET.get().floatValue() * none, "desert, wet season");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seasonMultiplierScalesTheClimate(GameTestHelper helper)
    {
        Holder<Biome> plains = biome(helper.getLevel(), Biomes.PLAINS);
        float plain = inSeason(helper, plains, Season.SubSeason.MID_SUMMER);
        try
        {
            CompatConfig.SERENE_SEASONS_SUMMER.set(1.5);
            helper.assertValueEqual(inSeason(helper, plains, Season.SubSeason.MID_SUMMER), 1.5F * plain, "plains in summer with summerMultiplier 1.5");
        }
        finally
        {
            CompatConfig.SERENE_SEASONS_SUMMER.set(1.0);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void biomeWithoutSeasonsDoesNotChange(GameTestHelper helper)
    {
        Holder<Biome> river = biome(helper.getLevel(), Biomes.RIVER);
        float none = withoutSeasons(helper, river);
        helper.assertValueEqual(inSeason(helper, river, Season.SubSeason.MID_SUMMER), none, "river in summer");
        helper.assertValueEqual(inSeason(helper, river, Season.SubSeason.MID_WINTER), none, "river in winter");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void switchedOffDoesNotChange(GameTestHelper helper)
    {
        Holder<Biome> plains = biome(helper.getLevel(), Biomes.PLAINS);
        Holder<Biome> desert = biome(helper.getLevel(), Biomes.DESERT);
        try
        {
            CompatConfig.SERENE_SEASONS_ENABLED.set(false);
            helper.assertValueEqual(inSeason(helper, plains, Season.SubSeason.MID_WINTER), withoutSeasons(helper, plains), "plains in winter, switched off");
            helper.assertValueEqual(inSeason(helper, desert, Season.SubSeason.MID_SUMMER), withoutSeasons(helper, desert), "desert in the dry season, switched off");
        }
        finally
        {
            CompatConfig.SERENE_SEASONS_ENABLED.set(true);
        }
        helper.succeed();
    }
}
