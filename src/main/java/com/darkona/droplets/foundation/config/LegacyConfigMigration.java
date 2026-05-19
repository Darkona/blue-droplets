package com.darkona.droplets.foundation.config;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.registry.LegacyIds;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One-time config migrations, run from the mod constructor before any config is registered:
 * {@code config/thirst/*.toml} (Thirst Was Taken) to {@code config/bluedroplets/}, then the old single files
 * ({@code common.toml}, {@code item_settings.toml}, {@code container.toml}, {@code keyword.toml}) to the files by concern.
 */
public final class LegacyConfigMigration
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** {old file, old path, new file, new path}; paths are dot-separated sections and key. */
    private static final String[][] MOVES = {
            {"common", "General.thirstDepletionModifier", "gameplay", "depletion.multiplier"},
            {"common", "General.thirstDepletionInPeace", "gameplay", "depletion.inPeaceful"},
            {"common", "General.netherThirstDeletionModifier", "gameplay", "depletion.netherMultiplier"},
            {"common", "General.fireResistanceDehydration", "gameplay", "depletion.fireResistancePercent"},
            {"common", "General.depletesWhenNausea", "gameplay", "depletion.nauseaDepletes"},
            {"common", "General.moveSlowWhenThirsty", "gameplay", "sprint.blockedWhenThirsty"},
            {"common", "General.DrinkRainWater", "gameplay", "drinking.rain"},
            {"common", "General.EnableLoot", "gameplay", "loot.enabled"},
            {"common", "Drinking Mechanics.waterBottleStacksize", "gameplay", "drinking.waterBottleStackSize"},
            {"common", "Drinking Mechanics.ExtraHydrationConvertToQuenched", "gameplay", "drinking.extraThirstToQuenched"},
            {"common", "Drinking Mechanics.dehydrationHaltsHealthRegen", "gameplay", "regeneration.haltedWhenThirsty"},
            {"common", "Drinking Mechanics.healthRegenDepletesHydration", "gameplay", "regeneration.depletesThirst"},
            {"common", "Drinking Mechanics.healthRegenDehydrationIsBiomeDependent", "gameplay", "regeneration.climateDependent"},
            {"common", "Drinking Mechanics.canDrinkByHand", "gameplay", "hand.enabled"},
            {"common", "Drinking Mechanics.DrinkBothHandNeeded", "gameplay", "hand.bothHandsEmpty"},
            {"common", "Drinking Mechanics.handDrinkingHydration", "gameplay", "hand.thirst"},
            {"common", "Drinking Mechanics.handDrinkingQuenched", "gameplay", "hand.quenched"},
            {"common", "Drinking Mechanics.handDrinkingCooldown", "gameplay", "hand.cooldownTicks"},
            {"common", "World.altitudeBands", "purity", "world.altitudeBands"},
            {"common", "World.altitudeRelativeToSeaLevel", "purity", "world.altitudeRelativeToSeaLevel"},
            {"common", "World.worldWaterBasePurity", "purity", "world.worldWaterBasePurity"},
            {"common", "World.saltWaterPurity", "purity", "world.saltWaterPurity"},
            {"common", "World.runningWaterPurificationAmount", "purity", "world.runningWaterPurificationAmount"},
            {"common", "World.stillWaterPurificationAmount", "purity", "world.stillWaterPurificationAmount"},
            {"common", "World.rainCauldronPurity", "purity", "world.rainCauldronPurity"},
            {"common", "World.dripstoneCauldronPurity", "purity", "world.dripstoneCauldronPurity"},
            {"common", "Purity-related Effects.defaultPurity", "purity", "general.defaultPurity"},
            {"common", "Purity-related Effects.quenchThirstWhenDebuffed", "purity", "general.quenchWhenDebuffed"},
            {"common", "Purity-related Effects.dirtyPoisonPercentage", "purity", "effects.dirtyPoisonPercentage"},
            {"common", "Purity-related Effects.dirtyNauseaPercentage", "purity", "effects.dirtyNauseaPercentage"},
            {"common", "Purity-related Effects.slightlyDirtyPoisonPercentage", "purity", "effects.slightlyDirtyPoisonPercentage"},
            {"common", "Purity-related Effects.slightlyDirtyNauseaPercentage", "purity", "effects.slightlyDirtyNauseaPercentage"},
            {"common", "Purity-related Effects.acceptablePoisonPercentage", "purity", "effects.acceptablePoisonPercentage"},
            {"common", "Purity-related Effects.acceptableNauseaPercentage", "purity", "effects.acceptableNauseaPercentage"},
            {"common", "Purity-related Effects.purifiedPoisonPercentage", "purity", "effects.purifiedPoisonPercentage"},
            {"common", "Purity-related Effects.purifiedNauseaPercentage", "purity", "effects.purifiedNauseaPercentage"},
            {"common", "Create compatibility.sandFilterFiltrationAmount", "compat", "create.sandFilterFiltrationAmount"},
            {"common", "Create compatibility.sandFilterMbPerTick", "compat", "create.sandFilterMbPerTick"},
            {"item_settings", "Drinks.drinks", "items", "overrides.drinks"},
            {"item_settings", "Foods.foods", "items", "overrides.foods"},
            {"item_settings", "Blacklist.itemsBlacklist", "items", "overrides.blacklist"},
            {"container", "Container.Containers", "items", "containers.containers"},
            {"keyword", "Keyword config.enableKeywordConfig", "items", "keywords.enabled"},
            {"keyword", "Keyword config.Default Hydration values.defaultDrinkHydration", "items", "keywords.drinkThirst"},
            {"keyword", "Keyword config.Default Hydration values.defaultDrinkQuenchness", "items", "keywords.drinkQuenched"},
            {"keyword", "Keyword config.Default Hydration values.defaultSoupHydration", "items", "keywords.soupThirst"},
            {"keyword", "Keyword config.Default Hydration values.defaultSoupQuenchness", "items", "keywords.soupQuenched"},
            {"keyword", "Keyword config.Default Hydration values.defaultFruitHydration", "items", "keywords.fruitThirst"},
            {"keyword", "Keyword config.Default Hydration values.defaultFruitQuenchness", "items", "keywords.fruitQuenched"},
            {"keyword", "Keyword config.Default Hydration values.Blacklisted Keywords.keyword_blacklist", "items", "keywords.blacklist"},
            {"keyword", "Keyword config.Default Hydration values.Drink Keywords.keyword_drink", "items", "keywords.drink"},
            {"keyword", "Keyword config.Default Hydration values.Soup Keywords.keyword_soup", "items", "keywords.soup"},
            {"keyword", "Keyword config.Default Hydration values.Fruit Keywords.keyword_fruit", "items", "keywords.fruit"},
    };

    private LegacyConfigMigration() {}

    public static void run()
    {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(BlueDroplets.ID);
        copyThirstFolder(dir);
        splitOldFiles(dir);
    }

    private static void copyThirstFolder(Path target)
    {
        Path legacy = target.resolveSibling(LegacyIds.LEGACY_NAMESPACE);
        if (Files.exists(target) || !Files.isDirectory(legacy))
            return;

        try
        {
            Files.createDirectories(target);
            try (DirectoryStream<Path> files = Files.newDirectoryStream(legacy, "*.toml"))
            {
                for (Path file : files)
                    Files.copy(file, target.resolve(file.getFileName()));
            }
            LOGGER.warn("Copied Thirst Was Taken config from {} to {}. The old folder is no longer read and can be deleted.", legacy, target);
        }
        catch (IOException e)
        {
            LOGGER.error("Could not copy Thirst Was Taken config from {} to {}; missing files will use defaults.", legacy, target, e);
        }
    }

    /**
     * For each new file that does not exist yet, copies the values of its keys from the old files; NeoForge adds the
     * missing keys and comments when it loads it. Old files that were read are renamed to {@code *.toml.old}.
     */
    private static void splitOldFiles(Path dir)
    {
        Map<String, Optional<Config>> oldFiles = new HashMap<>();
        Map<String, CommentedConfig> newFiles = new LinkedHashMap<>();
        for (String[] move : MOVES)
        {
            if (Files.exists(dir.resolve(move[2] + ".toml")))
                continue;
            Optional<Config> old = oldFiles.computeIfAbsent(move[0], name -> read(dir.resolve(name + ".toml")));
            Object value = old.map(config -> config.get(path(move[1]))).orElse(null);
            if (value != null)
                newFiles.computeIfAbsent(move[2], name -> TomlFormat.newConfig()).set(path(move[3]), value);
        }
        if (oldFiles.values().stream().noneMatch(Optional::isPresent))
            return;

        try
        {
            for (Map.Entry<String, CommentedConfig> file : newFiles.entrySet())
                try (Writer writer = Files.newBufferedWriter(dir.resolve(file.getKey() + ".toml")))
                {
                    TomlFormat.instance().createWriter().write(file.getValue(), writer);
                }
            for (Map.Entry<String, Optional<Config>> file : oldFiles.entrySet())
                if (file.getValue().isPresent())
                    Files.move(dir.resolve(file.getKey() + ".toml"), dir.resolve(file.getKey() + ".toml.old"), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Moved Blue Droplets settings from {} to {} in {}; the old files were renamed to *.toml.old and are no longer read.",
                    oldFiles.keySet(), newFiles.keySet(), dir);
        }
        catch (IOException e)
        {
            LOGGER.error("Could not move Blue Droplets settings to the new config files in {}; missing values will use defaults.", dir, e);
        }
    }

    private static Optional<Config> read(Path file)
    {
        if (!Files.isRegularFile(file))
            return Optional.empty();
        try (Reader reader = Files.newBufferedReader(file))
        {
            return Optional.of(TomlFormat.instance().createParser().parse(reader));
        }
        catch (Exception e)
        {
            LOGGER.error("Could not read old config file {}; its values will not be moved.", file, e);
            return Optional.empty();
        }
    }

    private static List<String> path(String dotted)
    {
        return Arrays.asList(dotted.split("\\."));
    }
}
