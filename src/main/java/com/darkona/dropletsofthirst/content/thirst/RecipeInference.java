package com.darkona.dropletsofthirst.content.thirst;

import com.darkona.dropletsofthirst.foundation.config.ItemsConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Estimates {thirst, quenched} of items that have no values from any other source, from the ingredients of the recipes
 * that make them (E13, off by default). Built once per table rebuild on the server; one memoized depth-first pass over
 * a result → recipes index. An ingredient is worth the average of its items that have values; a recipe is worth the
 * sum of its ingredients times the multiplier of its category, divided by the result count and capped. The best recipe
 * wins. Negative values (salty items) count as 0. Fluids in recipes are not seen (only the ingredients of
 * {@link Recipe#placementInfo()}); the result is the first one the recipe displays.
 */
public final class RecipeInference
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int[] NONE = {0, 0, -1};

    /**
     * Values of the other sources and the items that are never estimated nor passed on (blacklists, {@code no_thirst}).
     */
    public record Inputs(Map<Item, int[]> known, Set<Item> excluded)
    {
        public static final Inputs EMPTY = new Inputs(Map.of(), Set.of());
    }

    public record Result(Map<Item, int[]> drinks, Map<Item, int[]> foods, List<String> problems)
    {
        public static final Result EMPTY = new Result(Map.of(), Map.of(), List.of());
    }

    private record Entry(Identifier id, Identifier type, double multiplier, int count, List<Ingredient> ingredients) {}

    private final Inputs inputs;
    private final Set<String> namespaces = new HashSet<>();
    private final Map<Item, List<Entry>> byResult = new HashMap<>();
    private final Map<Identifier, Integer> unreadable = new TreeMap<>();
    private final List<String> problems = new ArrayList<>();
    private final Map<Item, int[]> memo = new HashMap<>();
    private final Set<Item> visiting = new HashSet<>();
    private final int maxDepth = ItemsConfig.INFERENCE_MAX_DEPTH.get();
    private final int maxThirst = ItemsConfig.INFERENCE_MAX_THIRST.get();
    private final int maxQuenched = ItemsConfig.INFERENCE_MAX_QUENCHED.get();
    private final boolean onlyConsumables = ItemsConfig.INFERENCE_ONLY_CONSUMABLES.get();
    private int recipes, cycles, cutoffs;

    private RecipeInference(Inputs inputs, RecipeManager recipeManager, HolderLookup.Provider registries)
    {
        this.inputs = inputs;
        for (String id : ItemsConfig.INFERENCE_BLACKLIST.get())
            if (id.startsWith("@"))
                namespaces.add(id.substring(1));
        Set<String> ignoredTypes = new HashSet<>();
        for (String id : ItemsConfig.INFERENCE_IGNORED_RECIPE_TYPES.get())
        {
            Identifier type = Identifier.tryParse(id);
            if (type != null && !BuiltInRegistries.RECIPE_TYPE.containsKey(type) && ModList.get().isLoaded(type.getNamespace()))
                problems.add("items.toml inference.ignoredRecipeTypes: unknown recipe type " + id);
            ignoredTypes.add(type == null ? id : type.toString());
        }

        ContextMap display = new ContextMap.Builder().withParameter(SlotDisplayContext.REGISTRIES, registries).create(SlotDisplayContext.CONTEXT);
        List<RecipeHolder<?>> holders = new ArrayList<>(recipeManager.getRecipes());
        holders.sort(Comparator.comparing(holder -> holder.id().identifier()));
        for (RecipeHolder<?> holder : holders)
        {
            Recipe<?> recipe = holder.value();
            Identifier type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            if (type == null || ignoredTypes.contains(type.toString()) || recipe.isSpecial())
                continue;
            try
            {
                ItemStack result = result(recipe, display);
                if (result.isEmpty())
                    continue;
                byResult.computeIfAbsent(result.getItem(), item -> new ArrayList<>())
                        .add(new Entry(holder.id().identifier(), type, multiplier(recipe.getType()), result.getCount(), recipe.placementInfo().ingredients()));
                recipes++;
            }
            catch (RuntimeException e)
            {
                unreadable.merge(type, 1, Integer::sum);
            }
        }
        unreadable.forEach((type, count) -> problems.add("inference: " + count + " recipe(s) of type " + type + " could not be read (skipped)"));
    }

    public static Result run(Inputs inputs, RecipeManager recipeManager, HolderLookup.Provider registries)
    {
        long start = System.nanoTime();
        RecipeInference inference = new RecipeInference(inputs, recipeManager, registries);
        int minThirst = ItemsConfig.INFERENCE_MIN_THIRST.get();
        Map<Item, int[]> drinks = new HashMap<>();
        Map<Item, int[]> foods = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM)
        {
            if (!inference.byResult.containsKey(item) || inputs.known().containsKey(item) || inference.skipped(item)
                    || (inference.onlyConsumables && !consumable(item)))
                continue;
            int[] values = inference.value(item, 0);
            if (values[0] >= minThirst)
                (isDrink(item) ? drinks : foods).put(item, values);
        }
        LOGGER.info("Estimated thirst values of {} items from {} recipes in {} ms ({} cycles and {} depth limits cut)",
                drinks.size() + foods.size(), inference.recipes, (System.nanoTime() - start) / 1_000_000, inference.cycles, inference.cutoffs);
        return new Result(drinks, foods, List.copyOf(inference.problems));
    }

    /**
     * Every recipe of the item with its ingredients' values, the estimate and why it would not be used.
     */
    public static List<String> explain(Item item, Inputs inputs, RecipeManager recipeManager, HolderLookup.Provider registries)
    {
        RecipeInference inference = new RecipeInference(inputs, recipeManager, registries);
        List<String> lines = new ArrayList<>();
        String name = BuiltInRegistries.ITEM.getKey(item).toString();
        int[] known = inputs.known().get(item);
        List<Entry> entries = inference.byResult.get(item);
        if (known != null)
            lines.add(name + " has " + known[0] + "/" + known[1] + " from items.toml, datapacks, code or keywords: nothing to estimate");
        else if (inference.skipped(item))
            lines.add(name + " is blacklisted (no_thirst tag, overrides.blacklist or inference.blacklist)");
        else if (entries == null)
            lines.add("No recipe makes " + name + " (special recipes and inference.ignoredRecipeTypes are skipped)");
        else
        {
            lines.add(name + ", thirst/quenched per recipe (an ingredient is the average of its items that have values):");
            int[] best = NONE;
            Identifier bestId = null;
            inference.visiting.add(item);
            for (Entry entry : entries)
            {
                List<String> trace = new ArrayList<>();
                int[] values = inference.compute(entry, 0, trace);
                lines.add(entry.id() + " (" + entry.type() + ", x" + format(entry.multiplier()) + ", makes " + entry.count() + "): " + values[0] + "/" + values[1]);
                lines.addAll(trace);
                if (better(values, best))
                {
                    best = values;
                    bestId = entry.id();
                }
            }
            lines.add("Estimate: " + best[0] + "/" + best[1] + (bestId == null ? "" : " from " + bestId)
                    + " (caps " + inference.maxThirst + "/" + inference.maxQuenched + ", depth " + inference.maxDepth + ")");
            if (inference.onlyConsumables && !consumable(item))
                lines.add("Not eaten or drunk (inference.onlyConsumables): only passed on to recipes that use it");
            else if (best[0] < ItemsConfig.INFERENCE_MIN_THIRST.get())
                lines.add("Below inference.minThirst: not used");
        }
        if (!ItemsConfig.INFERENCE.get())
            lines.add("inference.enabled is false: this is what it would give");
        return lines;
    }

    private int[] value(Item item, int depth)
    {
        int[] values = inputs.known().get(item);
        if (values != null)
            return values;
        if (skipped(item))
            return NONE;
        values = memo.get(item);
        if (values != null)
            return values;
        List<Entry> entries = byResult.get(item);
        if (entries == null)
            return NONE;
        if (depth >= maxDepth)
        {
            cutoffs++;
            return NONE;
        }
        if (!visiting.add(item))
        {
            cycles++;
            return NONE;
        }
        values = NONE;
        for (Entry entry : entries)
        {
            int[] candidate = compute(entry, depth, null);
            if (better(candidate, values))
                values = candidate;
        }
        visiting.remove(item);
        memo.put(item, values);
        return values;
    }

    private int[] compute(Entry entry, int depth, @Nullable List<String> trace)
    {
        double thirst = 0;
        double quenched = 0;
        for (Ingredient ingredient : entry.ingredients())
        {
            if (ingredient.isEmpty())
                continue;
            List<Holder<Item>> items = ingredient.items().toList();
            double sumThirst = 0;
            double sumQuenched = 0;
            int counted = 0;
            for (Holder<Item> item : items)
            {
                int[] values = value(item.value(), depth + 1);
                if (values[0] > 0 || values[1] > 0)
                {
                    sumThirst += Math.max(values[0], 0);
                    sumQuenched += Math.max(values[1], 0);
                    counted++;
                }
            }
            if (counted > 0)
            {
                thirst += sumThirst / counted;
                quenched += sumQuenched / counted;
            }
            if (trace != null)
                trace.add("  " + describe(items) + ": " + (counted == 0 ? "0/0" : format(sumThirst / counted) + "/" + format(sumQuenched / counted)));
        }
        double scale = entry.multiplier() / entry.count();
        return new int[]{Math.min(maxThirst, (int) Math.round(thirst * scale)), Math.min(maxQuenched, (int) Math.round(quenched * scale)), -1};
    }

    private boolean skipped(Item item)
    {
        return inputs.excluded().contains(item) || (!namespaces.isEmpty() && namespaces.contains(BuiltInRegistries.ITEM.getKey(item).getNamespace()));
    }

    private static boolean better(int[] a, int[] b)
    {
        return a[0] > b[0] || (a[0] == b[0] && a[1] > b[1]);
    }

    private static double multiplier(RecipeType<?> type)
    {
        if (type == RecipeType.CRAFTING)
            return ItemsConfig.INFERENCE_CRAFTING.get();
        if (type == RecipeType.SMELTING || type == RecipeType.BLASTING || type == RecipeType.SMOKING || type == RecipeType.CAMPFIRE_COOKING)
            return ItemsConfig.INFERENCE_COOKING.get();
        return ItemsConfig.INFERENCE_OTHER.get();
    }

    /**
     * The first item result the recipe shows (recipe book displays); empty for recipes that show none, like special ones.
     */
    private static ItemStack result(Recipe<?> recipe, ContextMap context)
    {
        for (RecipeDisplay display : recipe.display())
        {
            ItemStack result = display.result().resolveForFirstStack(context);
            if (!result.isEmpty())
                return result;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isDrink(Item item)
    {
        return item.getDefaultInstance().getUseAnimation() == ItemUseAnimation.DRINK;
    }

    private static boolean consumable(Item item)
    {
        ItemStack stack = item.getDefaultInstance();
        return stack.getUseAnimation() == ItemUseAnimation.DRINK || stack.has(DataComponents.FOOD);
    }

    private static String describe(List<Holder<Item>> items)
    {
        if (items.isEmpty())
            return "(matches nothing)";
        String first = BuiltInRegistries.ITEM.getKey(items.get(0).value()).toString();
        return items.size() == 1 ? first : first + " or " + (items.size() - 1) + " more";
    }

    private static String format(double value)
    {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
