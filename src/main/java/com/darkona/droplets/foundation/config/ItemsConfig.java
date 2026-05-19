package com.darkona.droplets.foundation.config;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code config/bluedroplets/items.toml}: per-item overrides, purity containers and keywords (was item_settings.toml,
 * container.toml and keyword.toml).
 */
public final class ItemsConfig
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends List<?>>> DRINKS;
    public static final ModConfigSpec.ConfigValue<List<? extends List<?>>> FOODS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONTAINERS;

    public static final ModConfigSpec.BooleanValue KEYWORDS;
    public static final ModConfigSpec.IntValue KEYWORD_DRINK_THIRST;
    public static final ModConfigSpec.IntValue KEYWORD_DRINK_QUENCHED;
    public static final ModConfigSpec.IntValue KEYWORD_SOUP_THIRST;
    public static final ModConfigSpec.IntValue KEYWORD_SOUP_QUENCHED;
    public static final ModConfigSpec.IntValue KEYWORD_FRUIT_THIRST;
    public static final ModConfigSpec.IntValue KEYWORD_FRUIT_QUENCHED;
    public static final ModConfigSpec.ConfigValue<String> KEYWORD_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<String> KEYWORD_DRINK;
    public static final ModConfigSpec.ConfigValue<String> KEYWORD_SOUP;
    public static final ModConfigSpec.ConfigValue<String> KEYWORD_FRUIT;

    public static final ModConfigSpec SPEC;

    static
    {
        BUILDER.comment("These entries win over the bluedroplets:drinks data map (datapacks) and over values registered by other mods.",
                "Format: [[\"namespace:item\" or \"#namespace:tag\", thirst, quenched], ...]").push("overrides");
        DRINKS = BUILDER.comment("Items that restore thirst when drunk").<List<?>>defineListAllowEmpty("drinks", List.of(), ItemsConfig::newEntry, ItemsConfig::checkEntry);
        FOODS = BUILDER.comment("Items that restore thirst when eaten").<List<?>>defineListAllowEmpty("foods", List.of(), ItemsConfig::newEntry, ItemsConfig::checkEntry);
        BLACKLIST = BUILDER.comment("Items that never restore thirst, whatever datapacks or other mods say: [\"namespace:item\", \"#namespace:tag\"]")
                .<String>defineListAllowEmpty("blacklist", List.of(), () -> "namespace:item", it -> it instanceof String);
        BUILDER.pop();

        BUILDER.push("containers");
        CONTAINERS = BUILDER.comment("Drinks that carry a water purity (added to the item tag bluedroplets:purity_containers, where the defaults live)",
                        "Format: [\"examplemod:example_item_1\", \"#examplemod:example_tag\"]")
                .<String>defineListAllowEmpty("containers", List.of(), () -> "namespace:item", it -> it instanceof String);
        BUILDER.pop();

        BUILDER.comment("Gives thirst values to items whose translation key matches a regular expression. Items with values from any other source are skipped").push("keywords");
        KEYWORDS = BUILDER.comment("Whether keywords are used").define("enabled", false);
        KEYWORD_DRINK_THIRST = BUILDER.defineInRange("drinkThirst", 10, 0, 20);
        KEYWORD_DRINK_QUENCHED = BUILDER.defineInRange("drinkQuenched", 14, 0, 20);
        KEYWORD_SOUP_THIRST = BUILDER.defineInRange("soupThirst", 4, 0, 20);
        KEYWORD_SOUP_QUENCHED = BUILDER.defineInRange("soupQuenched", 5, 0, 20);
        KEYWORD_FRUIT_THIRST = BUILDER.defineInRange("fruitThirst", 2, 0, 20);
        KEYWORD_FRUIT_QUENCHED = BUILDER.defineInRange("fruitQuenched", 3, 0, 20);
        KEYWORD_BLACKLIST = BUILDER.comment("Items matching this are never selected").define("blacklist", "(?:\\b|[^a-zA-Z])(dried|candied|leaf|leaves|gummy|crate|jam|sauce|bucket|seed|cookie|pie|bush|sapling|bean|curry|cake|candy)(?:\\b|[^a-zA-Z])");
        KEYWORD_DRINK = BUILDER.define("drink", "(?:\\b|[^a-zA-Z])(drink|juice|tea|soda|coffee|wine|beer|cider|yogurt|milkshake|smoothie)(?:\\b|[^a-zA-Z])");
        KEYWORD_SOUP = BUILDER.define("soup", "(?:\\b|[^a-zA-Z])(soup|stew|porridge)(?:\\b|[^a-zA-Z])");
        KEYWORD_FRUIT = BUILDER.define("fruit", "(?:\\b|[^a-zA-Z])(fruit|berry|berries|grape|orange|peach|pear|coconut|lemon|melon|cherry|apple)(?:\\b|[^a-zA-Z])");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ItemsConfig() {}

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
            LOGGER.warn("Skipping invalid entry {} in items.toml: expected [\"namespace:item\" or \"#namespace:tag\", thirst 0-20, quenched 0 or more]", entry);
        return false;
    }

    private static List<?> newEntry()
    {
        return Arrays.asList("namespace:item", 1, 1);
    }
}
