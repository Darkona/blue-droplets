package com.darkona.droplets.foundation.config;


import com.darkona.droplets.BlueDroplets;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ItemSettingsConfig
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final ModConfigSpec SPEC;
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends List<?>>> DRINKS;
    public static final ModConfigSpec.ConfigValue<List<? extends List<?>>> FOODS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEMS_BLACKLIST;

    static
    {
        BUILDER.push("Drinks")
                .comment("Overrides: items that recover thirst when drunk. Entries here win over the bluedroplets:drinks data map",
                        "(datapacks) and over values registered by other mods. Default values live in the data map.",
                        "Format: [[\"item-id-1\", hydration-amount, quenching-amount], [\"item-id-2\", hydration-amount, quenching-amount], ...etc]");
        DRINKS = BUILDER
                .<List<?>>defineListAllowEmpty("drinks", List.of(),
                        ItemSettingsConfig::newEntry, ItemSettingsConfig::checkEntry);

        BUILDER.pop();

        BUILDER.push("Foods")
                .comment("Overrides: items that recover thirst when eaten. Entries here win over the bluedroplets:drinks data map",
                        "(datapacks) and over values registered by other mods. Default values live in the data map.",
                        "Format: [[\"item-id-1\", hydration-amount, quenching-amount], [\"item-id-2\", hydration-amount, quenching-amount], ...etc]");
        FOODS = BUILDER
                .<List<?>>defineListAllowEmpty("foods", List.of(),
                        ItemSettingsConfig::newEntry, ItemSettingsConfig::checkEntry);

        BUILDER.pop();

        BUILDER.push("Blacklist");

        ITEMS_BLACKLIST = BUILDER.comment("A mod may have added thirst compatibility to an item via code. If you want to edit the thirst values",
                "of that item, add an entry in one of the first two lists. If instead you want to remove thirst support for that item, add an entry in this list",
                "Format: [\"examplemod:example_item_1\", \"examplemod:example_item_2\"]")
                .<String>defineListAllowEmpty("itemsBlacklist", Arrays.asList(
                                        "examplemod:example_item_1",
                                        "examplemod:example_item_2"
                        ),
                        () -> "namespace:item", it -> it instanceof String);



        SPEC = BUILDER.build();
    }

    /**
     * {@code ["namespace:item" or "#namespace:tag", thirst 0-20, quenched >= 0]}
     */
    public static boolean isValidEntry(Object entry)
    {
        return entry instanceof List<?> list && list.size() == 3 && list.get(0) instanceof String
                && list.get(1) instanceof Number thirst && thirst.doubleValue() >= 0 && thirst.doubleValue() <= 20
                && list.get(2) instanceof Number quenched && quenched.doubleValue() >= 0;
    }

    private static boolean checkEntry(Object entry)
    {
        if (isValidEntry(entry))
            return true;
        if (REPORTED.add(String.valueOf(entry)))
            LOGGER.warn("Skipping invalid entry {} in item_settings.toml: expected [\"namespace:item\" or \"#namespace:tag\", thirst 0-20, quenched 0 or more]", entry);
        return false;
    }

    private static List<?> newEntry()
    {
        return Arrays.asList("namespace:item", 1, 1);
    }

    public static void setup(ModContainer modContainer)
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path configFolder = Paths.get(configPath.toAbsolutePath().toString(), BlueDroplets.ID);

        try
        {
            Files.createDirectory(configFolder);
        }
        catch (Exception ignored) {}

        modContainer.registerConfig(ModConfig.Type.COMMON, SPEC, BlueDroplets.ID + "/item_settings.toml");
    }
}
