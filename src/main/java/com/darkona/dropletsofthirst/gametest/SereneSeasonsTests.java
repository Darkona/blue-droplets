package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.compat.coldsweat.ColdSweatCompat;
import com.darkona.dropletsofthirst.compat.sereneseasons.SereneSeasonsCompat;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import sereneseasons.api.season.Season;
import sereneseasons.season.SeasonHandler;
import sereneseasons.season.SeasonSavedData;
import sereneseasons.season.SeasonTime;

import static com.darkona.dropletsofthirst.gametest.TestSupport.assertValueEqual;

/**
 * Serene Seasons: the biome climate formula with the season's temperature and the tropical dry season. The runs with
 * Serene Seasons also have Cold Sweat, which turns the compat off in play, so these tests call
 * {@link ThirstHelper#biomeClimate} with the seasons forced on (the call the game makes without Cold Sweat) and check
 * the Cold Sweat switch apart. Each test sets the season and puts it back in the same call, so no other test sees it.
 * Registered by {@link DropletsGameTests} only when Serene Seasons is installed.
 */
@PrefixGameTestTemplate(false)
public class SereneSeasonsTests
{
    private static Holder<Biome> biome(ServerLevel level, ResourceKey<Biome> key)
    {
        return level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
    }

    /** Climate multiplier of {@code biome} in the middle of {@code season}, with seasons; the level's season is restored. */
    private static float inSeason(GameTestHelper helper, Holder<Biome> biome, Season.SubSeason season)
    {
        SeasonSavedData data = SeasonHandler.getSeasonSavedData(helper.getLevel());
        int ticks = data.seasonCycleTicks;
        try
        {
            data.seasonCycleTicks = SeasonTime.ZERO.getSubSeasonDuration() * season.ordinal();
            return ThirstHelper.biomeClimate(helper.getLevel(), biome, helper.absolutePos(BlockPos.ZERO), 1.0F, true);
        }
        finally
        {
            data.seasonCycleTicks = ticks;
        }
    }

    private static float withoutSeasons(GameTestHelper helper, Holder<Biome> biome)
    {
        return ThirstHelper.biomeClimate(helper.getLevel(), biome, helper.absolutePos(BlockPos.ZERO), 1.0F, false);
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
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

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void tropicalDrySeasonRaisesThirst(GameTestHelper helper)
    {
        Holder<Biome> desert = biome(helper.getLevel(), Biomes.DESERT);
        float none = withoutSeasons(helper, desert);
        // Mid summer is the early dry season in the tropics, mid spring the late wet one.
        assertValueEqual(helper, inSeason(helper, desert, Season.SubSeason.MID_SUMMER), CompatConfig.SERENE_SEASONS_TROPICAL_DRY.get().floatValue() * none, "desert, dry season");
        assertValueEqual(helper, inSeason(helper, desert, Season.SubSeason.MID_SPRING), CompatConfig.SERENE_SEASONS_TROPICAL_WET.get().floatValue() * none, "desert, wet season");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void seasonMultiplierScalesTheClimate(GameTestHelper helper)
    {
        Holder<Biome> plains = biome(helper.getLevel(), Biomes.PLAINS);
        float plain = inSeason(helper, plains, Season.SubSeason.MID_SUMMER);
        try
        {
            TestSupport.set(CompatConfig.SERENE_SEASONS_SUMMER, 1.5);
            assertValueEqual(helper, inSeason(helper, plains, Season.SubSeason.MID_SUMMER), 1.5F * plain, "plains in summer with summerMultiplier 1.5");
        }
        finally
        {
            TestSupport.set(CompatConfig.SERENE_SEASONS_SUMMER, 1.0);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void biomeWithoutSeasonsDoesNotChange(GameTestHelper helper)
    {
        Holder<Biome> river = biome(helper.getLevel(), Biomes.RIVER);
        float none = withoutSeasons(helper, river);
        assertValueEqual(helper, inSeason(helper, river, Season.SubSeason.MID_SUMMER), none, "river in summer");
        assertValueEqual(helper, inSeason(helper, river, Season.SubSeason.MID_WINTER), none, "river in winter");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void switchedOffDoesNotChange(GameTestHelper helper)
    {
        Holder<Biome> plains = biome(helper.getLevel(), Biomes.PLAINS);
        Holder<Biome> desert = biome(helper.getLevel(), Biomes.DESERT);
        try
        {
            TestSupport.set(CompatConfig.SERENE_SEASONS_ENABLED, false);
            assertValueEqual(helper, inSeason(helper, plains, Season.SubSeason.MID_WINTER), withoutSeasons(helper, plains), "plains in winter, switched off");
            assertValueEqual(helper, inSeason(helper, desert, Season.SubSeason.MID_SUMMER), withoutSeasons(helper, desert), "desert in the dry season, switched off");
        }
        finally
        {
            TestSupport.set(CompatConfig.SERENE_SEASONS_ENABLED, true);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void coldSweatTakesOver(GameTestHelper helper)
    {
        assertValueEqual(helper, SereneSeasonsCompat.ACTIVE, !ColdSweatCompat.LOADED, "seasons used only without Cold Sweat");
        helper.succeed();
    }
}
