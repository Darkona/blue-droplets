package com.darkona.droplets.foundation.config;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.ForgeConfigSpec;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code config/blue_droplets/items.toml}: per-item overrides, purity containers, keywords and recipe inference (was item_settings.toml,
 * container.toml and keyword.toml).
 */
public final class ItemsConfig
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> DRINKS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> FOODS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CONTAINERS;

    public static final ForgeConfigSpec.BooleanValue KEYWORDS;
    public static final ForgeConfigSpec.IntValue KEYWORD_DRINK_THIRST;
    public static final ForgeConfigSpec.IntValue KEYWORD_DRINK_QUENCHED;
    public static final ForgeConfigSpec.IntValue KEYWORD_SOUP_THIRST;
    public static final ForgeConfigSpec.IntValue KEYWORD_SOUP_QUENCHED;
    public static final ForgeConfigSpec.IntValue KEYWORD_FRUIT_THIRST;
    public static final ForgeConfigSpec.IntValue KEYWORD_FRUIT_QUENCHED;
    public static final ForgeConfigSpec.ConfigValue<String> KEYWORD_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<String> KEYWORD_DRINK;
    public static final ForgeConfigSpec.ConfigValue<String> KEYWORD_SOUP;
    public static final ForgeConfigSpec.ConfigValue<String> KEYWORD_FRUIT;

    public static final ForgeConfigSpec.IntValue SALTY_THIRST;
    public static final ForgeConfigSpec.IntValue SALTY_QUENCHED;
    public static final ForgeConfigSpec.BooleanValue INFERENCE;
    public static final ForgeConfigSpec.BooleanValue INFERENCE_ONLY_CONSUMABLES;
    public static final ForgeConfigSpec.IntValue INFERENCE_MAX_DEPTH;
    public static final ForgeConfigSpec.DoubleValue INFERENCE_CRAFTING;
    public static final ForgeConfigSpec.DoubleValue INFERENCE_COOKING;
    public static final ForgeConfigSpec.DoubleValue INFERENCE_OTHER;
    public static final ForgeConfigSpec.IntValue INFERENCE_MAX_THIRST;
    public static final ForgeConfigSpec.IntValue INFERENCE_MAX_QUENCHED;
    public static final ForgeConfigSpec.IntValue INFERENCE_MIN_THIRST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> INFERENCE_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> INFERENCE_IGNORED_RECIPE_TYPES;

    public static final ForgeConfigSpec SPEC;

    static
    {
        BUILDER.comment("These entries win over the blue_droplets:drinks data map (datapacks) and over values registered by other mods.",
                "Format: [[\"namespace:item\" or \"#namespace:tag\", thirst, quenched], ...]; thirst -20 to 20 and quenched -20 or more (negative values remove them)").push("overrides");
        DRINKS = BUILDER.comment("Items that restore thirst when drunk").<List<?>>defineListAllowEmpty(List.of("drinks"), () -> List.of(), ItemsConfig::checkEntry);
        FOODS = BUILDER.comment("Items that restore thirst when eaten").<List<?>>defineListAllowEmpty(List.of("foods"), () -> List.of(), ItemsConfig::checkEntry);
        BLACKLIST = BUILDER.comment("Items that never restore thirst, whatever datapacks or other mods say: [\"namespace:item\", \"#namespace:tag\"]")
                .<String>defineListAllowEmpty(List.of("blacklist"), () -> List.of(), it -> it instanceof String);
        BUILDER.pop();

        BUILDER.push("containers");
        CONTAINERS = BUILDER.comment("Drinks that carry a water purity (added to the item tag blue_droplets:purity_containers, where the defaults live)",
                        "Format: [\"examplemod:example_item_1\", \"#examplemod:example_tag\"]")
                .<String>defineListAllowEmpty(List.of("containers"), () -> List.of(), it -> it instanceof String);
        BUILDER.pop();

        BUILDER.comment("Items in the item tag blue_droplets:salty (empty by default) with no values from overrides, datapacks or other mods get these").push("salty");
        SALTY_THIRST = BUILDER.comment("Thirst they give (negative: they make the player thirstier)").defineInRange("thirstPenalty", -2, -20, 20);
        SALTY_QUENCHED = BUILDER.defineInRange("quenchedPenalty", -2, -20, 20);
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

        BUILDER.comment("Estimates thirst values from recipe ingredients for items that have none from any other source (shown as \"est.\").",
                "Computed when the world loads, on /reload and when this file changes; /blue_droplets infer <item> explains a result").push("inference");
        INFERENCE = BUILDER.comment("Whether values are estimated").define("enabled", false);
        INFERENCE_ONLY_CONSUMABLES = BUILDER.comment("Only items that are eaten or drunk get an estimate; other items still pass their value on to recipes that use them")
                .define("onlyConsumables", true);
        INFERENCE_MAX_DEPTH = BUILDER.comment("How many recipe steps are followed down from an item").defineInRange("maxDepth", 4, 1, 16);
        INFERENCE_CRAFTING = BUILDER.comment("Multiplier of crafting table recipes").defineInRange("craftingMultiplier", 1.0, 0.0, 10.0);
        INFERENCE_COOKING = BUILDER.comment("Multiplier of furnace, blast furnace, smoker and campfire recipes").defineInRange("cookingMultiplier", 1.0, 0.0, 10.0);
        INFERENCE_OTHER = BUILDER.comment("Multiplier of every other recipe type (other mods' machines and pots)").defineInRange("otherMultiplier", 1.0, 0.0, 10.0);
        INFERENCE_MAX_THIRST = BUILDER.comment("Highest estimated thirst").defineInRange("maxThirst", 8, 0, 20);
        INFERENCE_MAX_QUENCHED = BUILDER.comment("Highest estimated quenched").defineInRange("maxQuenched", 10, 0, 40);
        INFERENCE_MIN_THIRST = BUILDER.comment("Estimates below this thirst are dropped").defineInRange("minThirst", 1, 1, 20);
        INFERENCE_BLACKLIST = BUILDER.comment("Items never estimated nor passed on: [\"namespace:item\", \"#namespace:tag\", \"@namespace\"]")
                .<String>defineListAllowEmpty(List.of("blacklist"), () -> List.of(), it -> it instanceof String);
        INFERENCE_IGNORED_RECIPE_TYPES = BUILDER.comment("Recipe types that are not used: [\"namespace:type\"]")
                .<String>defineListAllowEmpty(List.of("ignoredRecipeTypes"), () -> List.of("minecraft:stonecutting", "minecraft:smithing"), it -> it instanceof String);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ItemsConfig() {}

    /**
     * {@code ["namespace:item" or "#namespace:tag", thirst -20..20, quenched >= -20]}
     */
    public static boolean isValidEntry(Object entry)
    {
        return entry instanceof List<?> list && list.size() == 3 && list.get(0) instanceof String
                && list.get(1) instanceof Number thirst && thirst.doubleValue() >= -20 && thirst.doubleValue() <= 20
                && list.get(2) instanceof Number quenched && quenched.doubleValue() >= -20;
    }

    private static boolean checkEntry(Object entry)
    {
        if (isValidEntry(entry))
            return true;
        if (REPORTED.add(String.valueOf(entry)))
            LOGGER.warn("Skipping invalid entry {} in items.toml: expected [\"namespace:item\" or \"#namespace:tag\", thirst -20 to 20, quenched -20 or more]", entry);
        return false;
    }
}
