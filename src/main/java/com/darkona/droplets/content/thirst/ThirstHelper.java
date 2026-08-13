package com.darkona.droplets.content.thirst;

import com.darkona.droplets.api.DrinkValueProvider;
import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.foundation.config.CompatConfig;
import com.darkona.droplets.foundation.config.ConfigCheck;
import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.foundation.mixin.BiomeAccessor;
import com.darkona.droplets.foundation.config.PurityConfig;
import com.darkona.droplets.foundation.config.ItemsConfig;
import com.darkona.droplets.foundation.config.SyncedValues;
import com.darkona.droplets.compat.coldsweat.ColdSweatCompat;
import com.darkona.droplets.compat.sereneseasons.SereneSeasonsCompat;
import com.darkona.droplets.content.data.DimensionWater;
import com.darkona.droplets.content.data.DrinkValues;
import com.darkona.droplets.content.data.DropletsDataMaps;
import com.darkona.droplets.content.data.DropletsTags;
import com.darkona.droplets.content.purity.ContainerWithPurity;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.core.NumberRows;
import com.darkona.droplets.core.ThirstConstants;
import com.darkona.droplets.foundation.common.event.RegisterThirstValueEvent;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Internal drink and food tables. Other mods use {@link com.darkona.droplets.api.DropletsAPI}.
 */
public class ThirstHelper
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final NumberRows TEMPERATURE_CURVE = new NumberRows(2);
    private static final NumberRows HUMIDITY_CURVE = new NumberRows(2);
    private static final NumberRows BODY_TEMPERATURE_CURVE = new NumberRows(2);

    private record CodeDrink(ItemLike item, int[] values) {}
    private record CodeProvider(ItemLike item, DrinkValueProvider provider) {}
    private record CodeContainer(@Nullable ItemLike empty, ItemLike filled) {}

    private static final List<CodeDrink> CODE_DRINKS = new CopyOnWriteArrayList<>();
    private static final List<CodeProvider> CODE_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<CodeContainer> CODE_CONTAINERS = new CopyOnWriteArrayList<>();

    /**
     * Table entry of an item whose values come from its {@link DrinkValueProvider}; synced like any other entry.
     */
    private static final int PROVIDED_THIRST = Integer.MIN_VALUE;
    private static final int[] PROVIDED = {PROVIDED_THIRST, 0, -1};
    private static final ThirstValues PROVIDED_VALUES = new ThirstValues(PROVIDED_THIRST, 0, -1, false);

    private record Table(Map<Item, int[]> drinks, Map<Item, int[]> foods, Set<Item> estimated, Map<Item, ThirstValues> values, Map<Item, DrinkValueProvider> providers)
    {
        static Table of(Map<Item, int[]> drinks, Map<Item, int[]> foods, Collection<Item> estimated)
        {
            Map<Item, ThirstValues> values = new HashMap<>();
            Set<Item> estimates = Set.copyOf(estimated);
            for (Map<Item, int[]> source : List.of(foods, drinks))
                source.forEach((item, v) -> values.put(item, v[0] == PROVIDED_THIRST ? PROVIDED_VALUES : new ThirstValues(v[0], v[1], v[2], estimates.contains(item))));
            return new Table(Map.copyOf(drinks), Map.copyOf(foods), estimates, Map.copyOf(values), values.containsValue(PROVIDED_VALUES) ? ThirstHelper.providers() : Map.of());
        }
    }

    private static Map<Item, DrinkValueProvider> providers()
    {
        Map<Item, DrinkValueProvider> providers = new HashMap<>();
        for (CodeProvider provider : CODE_PROVIDERS)
            providers.putIfAbsent(provider.item().asItem(), provider.provider());
        return Map.copyOf(providers);
    }

    public static void registerDrink(ItemLike item, int thirst, int quenched, int purity)
    {
        CODE_DRINKS.add(new CodeDrink(Objects.requireNonNull(item), new int[]{Mth.clamp(thirst, -ThirstConstants.MAX_THIRST, ThirstConstants.MAX_THIRST), Math.max(quenched, -ThirstConstants.MAX_THIRST), Mth.clamp(purity, -1, WaterPurity.MAX_PURITY)}));
    }

    public static void registerProvider(ItemLike item, DrinkValueProvider provider)
    {
        CODE_PROVIDERS.add(new CodeProvider(Objects.requireNonNull(item), Objects.requireNonNull(provider)));
    }

    public static void registerContainer(@Nullable ItemLike empty, ItemLike filled)
    {
        CODE_CONTAINERS.add(new CodeContainer(empty, Objects.requireNonNull(filled)));
    }

    private static final Table EMPTY = Table.of(Map.of(), Map.of(), Set.of());
    private static volatile Table table = EMPTY;
    private static volatile boolean serverTables;
    private static volatile List<String> unknownConfigIds = List.of();
    private static volatile RecipeInference.Inputs inferenceInputs = RecipeInference.Inputs.EMPTY;
    private static volatile RecipeInference.Result estimates = RecipeInference.Result.EMPTY;

    /**
     * Rebuilds the drink/food tables and the purity containers. Call once tags and data maps are bound; the tables
     * are replaced as a whole, never mutated. Values are {thirst, quenched, purity} with purity -1 when unset.
     * Each item takes its values from the first source that has it: blacklist and {@code blue_droplets:no_thirst} (no values),
     * {@code items.toml}, the {@code blue_droplets:drinks} data map, code ({@code DropletsAPI.registerDrink}, then
     * {@link RegisterThirstValueEvent}), {@link DrinkValueProvider}s, the {@code blue_droplets:salty} tag, keywords, and last the values estimated from recipes
     * ({@link RecipeInference}, server only: {@code recipes} is null on a remote client).
     */
    @SuppressWarnings("deprecation")
    public static void rebuild(@Nullable RecipeManager recipes, @Nullable RegistryAccess registries)
    {
        Tables tables = new Tables();
        List<ContainerWithPurity> containers = new ArrayList<>();
        Set<String> unknown = new LinkedHashSet<>();
        Set<String> absentMods = new LinkedHashSet<>();

        for (String id : ItemsConfig.BLACKLIST.get())
            resolve(id, tables.blocked::add, unknown, absentMods);
        for (Holder<Item> item : Registry.ITEM.getTagOrEmpty(DropletsTags.NO_THIRST))
            tables.blocked.add(item.value());

        readValues(ItemsConfig.DRINKS.get(), tables, false, unknown, absentMods);
        readValues(ItemsConfig.FOODS.get(), tables, true, unknown, absentMods);

        for (Map.Entry<ResourceKey<Item>, DrinkValues> entry : DropletsDataMaps.DRINKS.getDataMap().entrySet())
        {
            Item item = Registry.ITEM.get(entry.getKey());
            DrinkValues value = entry.getValue();
            tables.claim(item, new int[]{value.thirst(), value.quenched(), value.purity().orElse(-1)}, isFoodItem(item));
        }

        for (String id : ItemsConfig.CONTAINERS.get())
            resolve(id, item -> containers.add(new ContainerWithPurity(item)), unknown, absentMods);
        for (Holder<Item> item : Registry.ITEM.getTagOrEmpty(DropletsTags.PURITY_CONTAINERS))
            containers.add(new ContainerWithPurity(item.value()));

        Map<Item, int[]> codeDrinks = new LinkedHashMap<>();
        Map<Item, int[]> codeFoods = new LinkedHashMap<>();
        MinecraftForge.EVENT_BUS.post(new RegisterThirstValueEvent(codeDrinks, codeFoods, containers));
        for (CodeDrink drink : CODE_DRINKS)
        {
            Item item = drink.item().asItem();
            if (item != Items.AIR)
                tables.claim(item, drink.values(), isFoodItem(item));
        }
        codeDrinks.forEach((item, values) -> tables.claim(item, values, false));
        codeFoods.forEach((item, values) -> tables.claim(item, values, true));
        Set<Item> provided = new HashSet<>();
        for (Item item : providers().keySet())
            if (item != Items.AIR && tables.claim(item, PROVIDED, isFoodItem(item)))
                provided.add(item);
        for (CodeContainer container : CODE_CONTAINERS)
            containers.add(container.empty() == null ? new ContainerWithPurity(container.filled().asItem())
                    : new ContainerWithPurity(container.empty().asItem(), container.filled().asItem()));

        int[] salty = {ItemsConfig.SALTY_THIRST.get(), ItemsConfig.SALTY_QUENCHED.get(), -1};
        for (Holder<Item> item : Registry.ITEM.getTagOrEmpty(DropletsTags.SALTY))
            tables.claim(item.value(), salty, isFoodItem(item.value()));

        if (ItemsConfig.KEYWORDS.get())
            addKeywordItems(tables);

        Set<Item> estimated = new HashSet<>();
        if (recipes != null && registries != null)
        {
            Map<Item, int[]> known = new HashMap<>(tables.drinks);
            known.putAll(tables.foods);
            known.keySet().removeAll(provided);
            Set<Item> excluded = new HashSet<>(tables.blocked);
            excluded.addAll(provided);
            for (String id : ItemsConfig.INFERENCE_BLACKLIST.get())
                if (!id.startsWith("@"))
                    resolve(id, excluded::add, unknown, absentMods);
            inferenceInputs = new RecipeInference.Inputs(known, excluded);
            estimates = ItemsConfig.INFERENCE.get() ? RecipeInference.run(inferenceInputs, recipes, registries) : RecipeInference.Result.EMPTY;
            estimates.drinks().forEach((item, values) -> tables.claim(item, values, false));
            estimates.foods().forEach((item, values) -> tables.claim(item, values, true));
            estimated.addAll(estimates.drinks().keySet());
            estimated.addAll(estimates.foods().keySet());
        }

        unknownConfigIds = List.copyOf(unknown);
        if (!absentMods.isEmpty())
            LOGGER.debug("Skipped {} config entries of mods that are not installed: {}", absentMods.size(), absentMods);

        table = Table.of(tables.drinks, tables.foods, estimated);
        WaterPurity.setContainers(containers);
        ConfigCheck.report();
    }

    /**
     * Tables under construction: an item is in at most one of them, and blocked items in none.
     */
    private static final class Tables
    {
        final Set<Item> blocked = new HashSet<>();
        final Map<Item, int[]> drinks = new HashMap<>();
        final Map<Item, int[]> foods = new HashMap<>();

        boolean has(Item item)
        {
            return blocked.contains(item) || drinks.containsKey(item) || foods.containsKey(item);
        }

        /**
         * TOML entries: within a list the last entry for an item wins; an item in both lists stays a drink.
         */
        void override(Item item, int[] values, boolean food)
        {
            if (!blocked.contains(item) && !(food && drinks.containsKey(item)))
                (food ? foods : drinks).put(item, values);
        }

        boolean claim(Item item, int[] values, boolean food)
        {
            if (has(item))
                return false;
            (food ? foods : drinks).put(item, values);
            return true;
        }
    }

    /**
     * Ids and tags in the {@code items.toml} lists that matched nothing at the last rebuild.
     */
    public static List<String> unknownConfigIds()
    {
        return unknownConfigIds;
    }

    /**
     * Problems of the last recipe inference (unreadable recipes, unknown recipe types).
     */
    public static List<String> inferenceProblems()
    {
        return estimates.problems();
    }

    /**
     * Values and blacklists the last server-side rebuild gave the recipe inference; for {@code /blue_droplets infer}.
     */
    public static RecipeInference.Inputs inferenceInputs()
    {
        return inferenceInputs;
    }

    /**
     * Whether the item's values were estimated from recipes rather than given by a config, datapack or mod.
     */
    public static boolean isEstimated(ItemStack stack)
    {
        return table.estimated().contains(stack.getItem());
    }

    public static Set<Item> estimatedItems()
    {
        return table.estimated();
    }

    /**
     * Resolved drink values (item → {thirst, quenched, purity or -1}); immutable.
     */
    public static Map<Item, int[]> drinkTable()
    {
        return table.drinks();
    }

    /**
     * Resolved food values (item → {thirst, quenched, purity or -1}); immutable.
     */
    public static Map<Item, int[]> foodTable()
    {
        return table.foods();
    }

    /**
     * Client of a remote server: uses the tables resolved by the server; local rebuilds are skipped until {@link #clearServerTables()}.
     */
    public static void useServerTables(Map<Item, int[]> drinks, Map<Item, int[]> foods, List<Item> containers, List<Item> estimated)
    {
        table = Table.of(drinks, foods, estimated);
        WaterPurity.setContainers(containers.stream().map(ContainerWithPurity::new).toList());
        serverTables = true;
    }

    public static boolean hasServerTables()
    {
        return serverTables;
    }

    /**
     * Leaving a remote server: drops its tables; the next world or server fills them again.
     */
    public static void clearServerTables()
    {
        serverTables = false;
        table = EMPTY;
        WaterPurity.setContainers(List.of());
        SyncedValues.clear();
    }

    private static void readValues(List<? extends List<?>> entries, Tables tables, boolean food, Set<String> unknown, Set<String> absentMods)
    {
        for (List<?> entry : entries)
        {
            if (!ItemsConfig.isValidEntry(entry))
                continue;
            int[] values = {((Number) entry.get(1)).intValue(), ((Number) entry.get(2)).intValue(), -1};
            resolve((String) entry.get(0), item -> tables.override(item, values, food), unknown, absentMods);
        }
    }

    private static void resolve(String id, Consumer<Item> sink, Set<String> unknown, Set<String> absentMods)
    {
        boolean isTag = id.startsWith("#");
        ResourceLocation location = ResourceLocation.tryParse(isTag ? id.substring(1) : id);
        if (location != null)
        {
            if (isTag)
            {
                Optional<HolderSet.Named<Item>> tag = Registry.ITEM.getTag(TagKey.create(Registry.ITEM_REGISTRY, location));
                if (tag.isPresent())
                {
                    for (Holder<Item> item : tag.get())
                        sink.accept(item.value());
                    return;
                }
            }
            else
            {
                Optional<Holder<Item>> item = Registry.ITEM.getHolder(ResourceKey.create(Registry.ITEM_REGISTRY, location));
                if (item.isPresent())
                {
                    if (item.get().value() != Items.AIR)
                        sink.accept(item.get().value());
                    return;
                }
            }
        }

        if (location != null && !ModList.get().isLoaded(location.getNamespace()))
            absentMods.add(id);
        else
            unknown.add(id);
    }

    private static boolean isFoodItem(Item item)
    {
        return item.getDefaultInstance().getFoodProperties(null) != null;
    }

    private static void addKeywordItems(Tables tables)
    {
        Pattern blacklist = keyword(ItemsConfig.KEYWORD_BLACKLIST.get());
        Pattern drink = keyword(ItemsConfig.KEYWORD_DRINK.get());
        Pattern soup = keyword(ItemsConfig.KEYWORD_SOUP.get());
        Pattern fruit = keyword(ItemsConfig.KEYWORD_FRUIT.get());
        int[] drinkValues = {ItemsConfig.KEYWORD_DRINK_THIRST.get(), ItemsConfig.KEYWORD_DRINK_QUENCHED.get(), -1};
        int[] soupValues = {ItemsConfig.KEYWORD_SOUP_THIRST.get(), ItemsConfig.KEYWORD_SOUP_QUENCHED.get(), -1};
        int[] fruitValues = {ItemsConfig.KEYWORD_FRUIT_THIRST.get(), ItemsConfig.KEYWORD_FRUIT_QUENCHED.get(), -1};

        for (Item item : Registry.ITEM)
        {
            if (tables.has(item) || isFoodItem(item))
                continue;

            String name = item.getDescriptionId();
            if (matches(blacklist, name))
                continue;
            if (matches(drink, name))
                tables.claim(item, drinkValues, false);
            else if (matches(soup, name))
                tables.claim(item, soupValues, true);
            else if (matches(fruit, name))
                tables.claim(item, fruitValues, true);
        }
    }

    private static @Nullable Pattern keyword(String regex)
    {
        try
        {
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
        catch (PatternSyntaxException e)
        {
            return null;
        }
    }

    private static boolean matches(@Nullable Pattern pattern, String name)
    {
        return pattern != null && pattern.matcher(name).find();
    }

    public static boolean itemRestoresThirst(ItemStack itemStack)
    {
        return valuesOf(itemStack) != null;
    }

    /**
     * Whether this player may drink this item. Every player can on 1.19.2: Supernatural's vampires, the only ones
     * that are limited to blood, are not supported on this version.
     */
    public static boolean playerRestoresThirst(ItemStack itemStack, Player player)
    {
        return true;
    }

    public static boolean isDrink(ItemStack itemStack)
    {
        return table.drinks().containsKey(itemStack.getItem());
    }

    public static boolean isFood(ItemStack itemStack)
    {
        return table.foods().containsKey(itemStack.getItem());
    }

    /**
     * Subscribe #{@link RegisterThirstValueEvent} to use the api.
     * */
    @Deprecated(forRemoval = true, since = "1.3.8")
    @SuppressWarnings("unused")
    public static void addFood(Item item, int thirst, int quenched) {}

    /**
     * Subscribe #{@link RegisterThirstValueEvent} to use the api.
     * */
    @Deprecated(forRemoval = true, since = "1.3.8")
    @SuppressWarnings("unused")
    public static void addDrink(Item item, int thirst, int quenched) {}

    public static int getThirst(ItemStack itemStack)
    {
        ThirstValues values = valuesOf(itemStack);
        return values == null ? 0 : values.thirst();
    }

    public static int getQuenched(ItemStack itemStack)
    {
        ThirstValues values = valuesOf(itemStack);
        return values == null ? 0 : values.quenched();
    }

    /**
     * Purity of this drink when the stack stores none ({@code purity} in the data map), or -1.
     */
    public static int getDrinkPurity(ItemStack itemStack)
    {
        ThirstValues values = valuesOf(itemStack);
        return values == null ? -1 : values.purity();
    }

    /**
     * Resolved values of the stack, or null; allocates nothing itself (a {@link DrinkValueProvider} may).
     */
    public static @Nullable ThirstValues valuesOf(ItemStack stack)
    {
        Table current = table;
        ThirstValues values = current.values().get(stack.getItem());
        if (values != PROVIDED_VALUES)
            return values;
        DrinkValueProvider provider = current.providers().get(stack.getItem());
        return provider == null ? null : provider.values(stack);
    }

    private record PureValues(ThirstValues base, int thirst, int quenched, ThirstValues values) {}

    private static volatile PureValues lastPure = new PureValues(new ThirstValues(0, 0), 0, 0, new ThirstValues(0, 0));

    /**
     * What drinking or eating the stack gives: {@link #valuesOf} plus, for a water container of pure water, the
     * {@code pureWater} bonus. The last result with a bonus is kept, so tooltips and the HUD preview allocate
     * nothing while they show the same stack.
     */
    public static @Nullable ThirstValues drinkValuesOf(ItemStack stack)
    {
        ThirstValues values = valuesOf(stack);
        if (values == null || !WaterPurity.isWaterFilledContainer(stack))
            return values;
        int purity = WaterPurity.getPurity(stack);
        int thirst = WaterPurity.waterThirstBonus(purity);
        int quenched = WaterPurity.waterQuenchedBonus(purity);
        if (thirst == 0 && quenched == 0)
            return values;
        PureValues last = lastPure;
        if (last.thirst() != thirst || last.quenched() != quenched || !last.base().equals(values))
            lastPure = last = new PureValues(values, thirst, quenched,
                    new ThirstValues(values.thirst() + thirst, values.quenched() + quenched, values.purity(), values.estimated()));
        return last.values();
    }

    public static int getPurity(ItemStack item)
    {
        return WaterPurity.getPurity(item);
    }

    public static float getExhaustionFireProtModifier(Player player)
    {
        int levels = 0;
        for(ItemStack armor : player.getArmorSlots())
            levels += armor.getEnchantmentLevel(Enchantments.FIRE_PROTECTION);
        return Math.max(0.0f, 1.0f - Math.min(levels, GameplayConfig.FIRE_PROTECTION_MAX_LEVELS.get()) * GameplayConfig.FIRE_PROTECTION_PER_LEVEL.get().floatValue());
    }

    public static float getExhaustionFireResistanceModifier(Player player){
        if(player.hasEffect(MobEffects.FIRE_RESISTANCE)){
            return (float) GameplayConfig.FIRE_RESISTANCE_PERCENT.get() /100;
        }else return 1.0f;
    }

    /**
     * Climate multiplier: the dimension type's {@code thirst_multiplier} ({@code blue_droplets:dimension_water}) or
     * {@code netherMultiplier} in ultra-warm dimensions replace it; otherwise {@code depletion.multiplier} times the
     * Cold Sweat body temperature curve ({@code coldsweat.useBodyTemperature}), or else the LEGACY formula or the
     * CURVE multipliers of biome temperature and downfall ({@link #biomeClimate}), with the seasons of Serene Seasons.
     */
    public static float getExhaustionBiomeModifier(Player player)
    {
        Level level = player.level;
        DimensionWater dimension = DropletsDataMaps.DIMENSION_WATER.get(level.dimensionTypeRegistration());
        if (dimension != null && dimension.thirstMultiplier().isPresent())
            return dimension.thirstMultiplier().get();
        if (level.dimensionType().ultraWarm())
            return GameplayConfig.NETHER_MULTIPLIER.get().floatValue();

        float multiplier = GameplayConfig.DEPLETION_MULTIPLIER.get().floatValue();
        if (ColdSweatCompat.LOADED && CompatConfig.COLD_SWEAT_BODY_TEMPERATURE.get())
            return multiplier * (float) NumberRows.curve(BODY_TEMPERATURE_CURVE.get(CompatConfig.COLD_SWEAT_BODY_TEMPERATURE_CURVE.get()), ColdSweatCompat.bodyTemperature(player));

        BlockPos pos = player.getOnPos();
        return biomeClimate(level, level.getBiome(pos), pos, multiplier, SereneSeasonsCompat.ACTIVE);
    }

    /**
     * The biome formula of {@link #getExhaustionBiomeModifier}, LEGACY or CURVE, times {@code multiplier}. With
     * {@code seasons} (Serene Seasons loaded) the temperature is the one of the current season, and a tropical biome's
     * dry season adds its multiplier. The temperature is vanilla's at {@code pos}, cooled with height, with or without seasons.
     */
    public static float biomeClimate(Level level, Holder<Biome> biomeHolder, BlockPos pos, float multiplier, boolean seasons)
    {
        Biome biome = biomeHolder.value();
        // Vanilla's temperature at pos, cooled with height; Serene Seasons' one includes that too, so the formula agrees with and without it.
        float base = ((BiomeAccessor) (Object) biome).blue_droplets$getTemperature(pos);
        float temperature = seasons ? SereneSeasonsCompat.temperature(level, biomeHolder, pos, base) : base;
        float season = seasons ? SereneSeasonsCompat.seasonMultiplier(level, biomeHolder) : 1.0F;
        float downfall = biome.getModifiedClimateSettings().downfall();
        if (GameplayConfig.CLIMATE_FORMULA.get() == GameplayConfig.ClimateFormula.CURVE)
            return season * multiplier * (float) (NumberRows.curve(TEMPERATURE_CURVE.get(GameplayConfig.TEMPERATURE_CURVE.get()), temperature)
                    * NumberRows.curve(HUMIDITY_CURVE.get(GameplayConfig.HUMIDITY_CURVE.get()), downfall));

        //humidity range: 0 - 0.8 == 0.8 midpoint: 0.4
        float humidity = downfall + 0.6f;
        if(humidity <= 0.6)
            humidity += 0.5;

        //temperature range: -0.8 - 2 == 2.8 midpoint: 0.8
        float temp = temperature + 0.2f;
        if(temp <= 0)
            temp = (float) Math.exp(temp);
        else if(temp > 1)
            temp /= 2;

        float thirstModifier = multiplier * (temp / humidity);
        if(thirstModifier < 1)
            thirstModifier = 1 - (1 - thirstModifier) * GameplayConfig.LEGACY_HARSHNESS.get().floatValue();
        return season * thirstModifier;
    }

    /**
     * Whether the player is in a hot climate for {@code hotDirtyWater}: ultra-warm dimension, warm biome, or Cold Sweat body temperature.
     */
    public static boolean isHotClimate(Player player)
    {
        Level level = player.level;
        if (level.dimensionType().ultraWarm())
            return true;
        if (level.getBiome(player.getOnPos()).value().getBaseTemperature() >= PurityConfig.HOT_DIRTY_WATER_MIN_BIOME_TEMPERATURE.get())
            return true;
        return ColdSweatCompat.LOADED && PurityConfig.HOT_DIRTY_WATER_COLD_SWEAT.get()
                && ColdSweatCompat.bodyTemperature(player) > PurityConfig.HOT_DIRTY_WATER_COLD_SWEAT_MIN_BODY_TEMP.get();
    }
}
