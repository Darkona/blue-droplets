package com.darkona.droplets.content.thirst;

import com.darkona.droplets.foundation.config.CompatConfig;
import com.darkona.droplets.foundation.config.ConfigCheck;
import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.foundation.config.ItemsConfig;
import com.darkona.droplets.foundation.config.SyncedValues;
import com.darkona.droplets.compat.coldsweat.ColdSweatCompat;
import com.darkona.droplets.compat.supernatural.SupernaturalCompat;
import com.darkona.droplets.content.data.DimensionWater;
import com.darkona.droplets.content.data.DrinkValues;
import com.darkona.droplets.content.data.DropletsDataMaps;
import com.darkona.droplets.content.data.DropletsTags;
import com.darkona.droplets.content.purity.ContainerWithPurity;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.core.NumberRows;
import com.darkona.droplets.foundation.common.event.RegisterThirstValueEvent;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    private record Table(Map<Item, int[]> drinks, Map<Item, int[]> foods, Set<Item> estimated) {}

    private static final Table EMPTY = new Table(Map.of(), Map.of(), Set.of());
    private static volatile Table table = EMPTY;
    private static volatile boolean serverTables;
    private static volatile List<String> unknownConfigIds = List.of();
    private static volatile RecipeInference.Inputs inferenceInputs = RecipeInference.Inputs.EMPTY;
    private static volatile RecipeInference.Result estimates = RecipeInference.Result.EMPTY;
    private static @Nullable RegistryAccess fireProtectionAccess;
    private static @Nullable Holder<Enchantment> fireProtectionHolder;

    /**
     * Rebuilds the drink/food tables and the purity containers. Call once tags and data maps are bound; the tables
     * are replaced as a whole, never mutated. Values are {thirst, quenched, purity} with purity -1 when unset.
     * Each item takes its values from the first source that has it: blacklist and {@code bluedroplets:no_thirst} (no values),
     * {@code items.toml}, the {@code bluedroplets:drinks} data map, {@link RegisterThirstValueEvent}, keywords, and last
     * the values estimated from recipes ({@link RecipeInference}, server only: {@code recipes} is null on a remote client).
     */
    public static void rebuild(@Nullable RecipeManager recipes, @Nullable HolderLookup.Provider registries)
    {
        Tables tables = new Tables();
        List<ContainerWithPurity> containers = new ArrayList<>();
        Set<String> unknown = new LinkedHashSet<>();
        Set<String> absentMods = new LinkedHashSet<>();

        for (String id : ItemsConfig.BLACKLIST.get())
            resolve(id, tables.blocked::add, unknown, absentMods);
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(DropletsTags.NO_THIRST))
            tables.blocked.add(item.value());

        readValues(ItemsConfig.DRINKS.get(), tables, false, unknown, absentMods);
        readValues(ItemsConfig.FOODS.get(), tables, true, unknown, absentMods);

        for (Map.Entry<ResourceKey<Item>, DrinkValues> entry : BuiltInRegistries.ITEM.getDataMap(DropletsDataMaps.DRINKS).entrySet())
        {
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            DrinkValues value = entry.getValue();
            tables.claim(item, new int[]{value.thirst(), value.quenched(), value.purity().orElse(-1)}, isFoodItem(item));
        }

        for (String id : ItemsConfig.CONTAINERS.get())
            resolve(id, item -> containers.add(new ContainerWithPurity(item)), unknown, absentMods);
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(DropletsTags.PURITY_CONTAINERS))
            containers.add(new ContainerWithPurity(item.value()));

        Map<Item, int[]> codeDrinks = new LinkedHashMap<>();
        Map<Item, int[]> codeFoods = new LinkedHashMap<>();
        NeoForge.EVENT_BUS.post(new RegisterThirstValueEvent(codeDrinks, codeFoods, containers));
        codeDrinks.forEach((item, values) -> tables.claim(item, values, false));
        codeFoods.forEach((item, values) -> tables.claim(item, values, true));

        if (ItemsConfig.KEYWORDS.get())
            addKeywordItems(tables);

        Set<Item> estimated = new HashSet<>();
        if (recipes != null && registries != null)
        {
            Map<Item, int[]> known = new HashMap<>(tables.drinks);
            known.putAll(tables.foods);
            Set<Item> excluded = new HashSet<>(tables.blocked);
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

        table = new Table(Map.copyOf(tables.drinks), Map.copyOf(tables.foods), Set.copyOf(estimated));
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

        void claim(Item item, int[] values, boolean food)
        {
            if (!has(item))
                (food ? foods : drinks).put(item, values);
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
     * Values and blacklists the last server-side rebuild gave the recipe inference; for {@code /bluedroplets infer}.
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
        table = new Table(Map.copyOf(drinks), Map.copyOf(foods), Set.copyOf(estimated));
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
                Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, location));
                if (tag.isPresent())
                {
                    for (Holder<Item> item : tag.get())
                        sink.accept(item.value());
                    return;
                }
            }
            else
            {
                Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.getHolder(location);
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

        for (Item item : BuiltInRegistries.ITEM)
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
        return values(itemStack.getItem()) != null;
    }

    public static boolean playerRestoresThirst(ItemStack itemStack, Player player)
    {
        return SupernaturalCompat.canDrinkItem(itemStack, player);
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
        int[] values = values(itemStack.getItem());
        return values == null ? 0 : values[0];
    }

    public static int getQuenched(ItemStack itemStack)
    {
        int[] values = values(itemStack.getItem());
        return values == null ? 0 : values[1];
    }

    /**
     * Purity of this drink when the stack stores none ({@code purity} in the data map), or -1.
     */
    public static int getDrinkPurity(ItemStack itemStack)
    {
        int[] values = values(itemStack.getItem());
        return values == null ? -1 : values[2];
    }

    private static int[] values(Item item)
    {
        Table current = table;
        int[] values = current.drinks().get(item);
        return values != null ? values : current.foods().get(item);
    }

    public static int getPurity(ItemStack item)
    {
        return WaterPurity.getPurity(item);
    }

    public static float getExhaustionFireProtModifier(Player player)
    {
        Holder<Enchantment> fireProtection = fireProtection(player.level());
        if(fireProtection == null)
            return 1.0f;
        int levels = 0;
        for(ItemStack armor : player.getArmorSlots())
            levels += armor.getEnchantmentLevel(fireProtection);
        return Math.max(0.0f, 1.0f - Math.min(levels, GameplayConfig.FIRE_PROTECTION_MAX_LEVELS.get()) * GameplayConfig.FIRE_PROTECTION_PER_LEVEL.get().floatValue());
    }

    private static @Nullable Holder<Enchantment> fireProtection(Level level)
    {
        RegistryAccess access = level.registryAccess();
        if(access != fireProtectionAccess)
        {
            fireProtectionHolder = access.registryOrThrow(Registries.ENCHANTMENT).getHolder(Enchantments.FIRE_PROTECTION).orElse(null);
            fireProtectionAccess = access;
        }
        return fireProtectionHolder;
    }

    public static float getExhaustionFireResistanceModifier(Player player){
        if(player.hasEffect(MobEffects.FIRE_RESISTANCE)){
            return (float) GameplayConfig.FIRE_RESISTANCE_PERCENT.get() /100;
        }else return 1.0f;
    }

    /**
     * Climate multiplier: the dimension type's {@code thirst_multiplier} ({@code bluedroplets:dimension_water}) or
     * {@code netherMultiplier} in ultra-warm dimensions replace it; otherwise {@code depletion.multiplier} times the
     * LEGACY formula or the CURVE multipliers of biome temperature (Cold Sweat: body temperature / 100) and downfall.
     */
    public static float getExhaustionBiomeModifier(Player player)
    {
        Level level = player.level();
        DimensionWater dimension = level.dimensionTypeRegistration().getData(DropletsDataMaps.DIMENSION_WATER);
        if (dimension != null && dimension.thirstMultiplier().isPresent())
            return dimension.thirstMultiplier().get();
        if (level.dimensionType().ultraWarm())
            return GameplayConfig.NETHER_MULTIPLIER.get().floatValue();

        Biome biome = level.getBiome(player.getOnPos()).value();
        float downfall = biome.getModifiedClimateSettings().downfall();
        boolean bodyTemperature = ColdSweatCompat.LOADED && CompatConfig.COLD_SWEAT_BODY_TEMPERATURE.get();
        float multiplier = GameplayConfig.DEPLETION_MULTIPLIER.get().floatValue();

        if (GameplayConfig.CLIMATE_FORMULA.get() == GameplayConfig.ClimateFormula.CURVE)
        {
            float temp = bodyTemperature ? (float) (ColdSweatCompat.bodyTemperature(player) / 100f) : biome.getBaseTemperature();
            return multiplier * (float) (NumberRows.curve(TEMPERATURE_CURVE.get(GameplayConfig.TEMPERATURE_CURVE.get()), temp)
                    * NumberRows.curve(HUMIDITY_CURVE.get(GameplayConfig.HUMIDITY_CURVE.get()), downfall));
        }

        //humidity range: 0 - 0.8 == 0.8 midpoint: 0.4
        float humidity = downfall + 0.6f;
        if(humidity <= 0.6)
            humidity += 0.5;

        //temperature range: -0.8 - 2 == 2.8 midpoint: 0.8
        float temp = biome.getBaseTemperature() + 0.2f;
        if(bodyTemperature)
            temp = (float) (ColdSweatCompat.bodyTemperature(player) / 100f);
        else if(temp <= 0)
            temp = (float) Math.exp(temp);
        else if(temp > 1)
            temp /= 2;

        float thirstModifier = multiplier * (temp / humidity);
        if(thirstModifier < 1)
            thirstModifier = 1 - (1 - thirstModifier) * GameplayConfig.LEGACY_HARSHNESS.get().floatValue();
        return thirstModifier;
    }
}
