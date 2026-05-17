package com.darkona.droplets.api;

import com.darkona.droplets.compat.coldsweat.ColdSweatCompat;
import com.darkona.droplets.compat.supernatural.SupernaturalCompat;
import com.darkona.droplets.content.data.DrinkValues;
import com.darkona.droplets.content.data.DropletsDataMaps;
import com.darkona.droplets.content.data.DropletsTags;
import com.darkona.droplets.content.purity.ContainerWithPurity;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.core.ThirstConstants;
import com.darkona.droplets.foundation.common.event.RegisterThirstValueEvent;
import com.darkona.droplets.foundation.config.CommonConfig;
import com.darkona.droplets.foundation.config.ContainerConfig;
import com.darkona.droplets.foundation.config.ItemSettingsConfig;
import com.darkona.droplets.foundation.config.KeyWordConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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

public class ThirstHelper
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float MODIFIER_HARSHNESS = 0.5f;

    private record Table(Map<Item, int[]> drinks, Map<Item, int[]> foods) {}

    private static final Table EMPTY = new Table(Map.of(), Map.of());
    private static volatile Table table = EMPTY;
    private static volatile boolean serverTables;
    private static @Nullable RegistryAccess fireProtectionAccess;
    private static @Nullable Holder<Enchantment> fireProtectionHolder;

    /**
     * Rebuilds the drink/food tables and the purity containers. Call once tags and data maps are bound; the tables
     * are replaced as a whole, never mutated. Values are {thirst, quenched, purity} with purity -1 when unset.
     * Each item takes its values from the first source that has it: blacklist and {@code bluedroplets:no_thirst} (no values),
     * {@code item_settings.toml}, the {@code bluedroplets:drinks} data map, {@link RegisterThirstValueEvent}, keywords.
     */
    public static void rebuild()
    {
        Tables tables = new Tables();
        List<ContainerWithPurity> containers = new ArrayList<>();
        Set<String> unknown = new LinkedHashSet<>();
        Set<String> absentMods = new LinkedHashSet<>();

        for (String id : ItemSettingsConfig.ITEMS_BLACKLIST.get())
            resolve(id, tables.blocked::add, unknown, absentMods);
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(DropletsTags.NO_THIRST))
            tables.blocked.add(item.value());

        readValues(ItemSettingsConfig.DRINKS.get(), tables, false, unknown, absentMods);
        readValues(ItemSettingsConfig.FOODS.get(), tables, true, unknown, absentMods);

        for (Map.Entry<ResourceKey<Item>, DrinkValues> entry : BuiltInRegistries.ITEM.getDataMap(DropletsDataMaps.DRINKS).entrySet())
        {
            Item item = BuiltInRegistries.ITEM.get(entry.getKey());
            DrinkValues value = entry.getValue();
            tables.claim(item, new int[]{value.thirst(), value.quenched(), value.purity().orElse(-1)}, isFoodItem(item));
        }

        for (String id : ContainerConfig.CONTAINERS.get())
            resolve(id, item -> containers.add(new ContainerWithPurity(item)), unknown, absentMods);

        Map<Item, int[]> codeDrinks = new LinkedHashMap<>();
        Map<Item, int[]> codeFoods = new LinkedHashMap<>();
        NeoForge.EVENT_BUS.post(new RegisterThirstValueEvent(codeDrinks, codeFoods, containers));
        codeDrinks.forEach((item, values) -> tables.claim(item, values, false));
        codeFoods.forEach((item, values) -> tables.claim(item, values, true));

        if (KeyWordConfig.ENABLE_KEYWORD_CONFIG.get())
            addKeywordItems(tables);

        if (!unknown.isEmpty())
            LOGGER.warn("Skipped {} config entries with no such item or tag: {}", unknown.size(), unknown);
        if (!absentMods.isEmpty())
            LOGGER.debug("Skipped {} config entries of mods that are not installed: {}", absentMods.size(), absentMods);

        table = new Table(Map.copyOf(tables.drinks), Map.copyOf(tables.foods));
        WaterPurity.setContainers(containers);
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
    public static void useServerTables(Map<Item, int[]> drinks, Map<Item, int[]> foods, List<Item> containers)
    {
        table = new Table(Map.copyOf(drinks), Map.copyOf(foods));
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
        WaterPurity.setServerDefaultPurity(-1);
    }

    private static void readValues(List<? extends List<?>> entries, Tables tables, boolean food, Set<String> unknown, Set<String> absentMods)
    {
        for (List<?> entry : entries)
        {
            if (!ItemSettingsConfig.isValidEntry(entry))
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
        Pattern blacklist = keyword(KeyWordConfig.KEYWORD_BLACKLIST.get());
        Pattern drink = keyword(KeyWordConfig.KEYWORD_DRINK.get());
        Pattern soup = keyword(KeyWordConfig.KEYWORD_SOUP.get());
        Pattern fruit = keyword(KeyWordConfig.KEYWORD_FRUIT.get());
        int[] drinkValues = {KeyWordConfig.getDrinkHydration(), KeyWordConfig.getDrinkQuenchness(), -1};
        int[] soupValues = {KeyWordConfig.getSoupHydration(), KeyWordConfig.getSoupQuenchness(), -1};
        int[] fruitValues = {KeyWordConfig.getFruitHydration(), KeyWordConfig.getFruitQuenchness(), -1};

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
            LOGGER.warn("Ignoring invalid keyword pattern '{}': {}", regex, e.getDescription());
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
        return 1.0f - Math.min(levels, ThirstConstants.FIRE_PROTECTION_MAX_LEVELS) * ThirstConstants.FIRE_PROTECTION_REDUCTION_PER_LEVEL;
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
            return (float) CommonConfig.FIRE_RESISTANCE_DEHYDRATION.get() /100;
        }else return 1.0f;
    }

    /**
     * Calculates the thirst depletion speed modifier based on the player's
     * temperature and humidity. If the mod "Cold Sweat" is present, the temperature used is
     * the one calculated from the mod, otherwise both parameters are entirely
     * dependent on the biome the player is standing in.
     */
    public static float getExhaustionBiomeModifier(Player player)
    {
        BlockPos pos = player.getOnPos();
        Level level = player.level();

        if(level.dimensionType().ultraWarm())
            return CommonConfig.NETHER_THIRST_DEPLETION_MODIFIER.get().floatValue();
        else
        {
            Biome biome = level.getBiome(pos).value();

            //humidity range: 0 - 0.8 == 0.8 midpoint: 0.4
            float humidity = biome.getModifiedClimateSettings().downfall() + 0.6f;
            if(humidity <= 0.6)
                humidity += 0.5;

            //temperature range: -0.8 - 2 == 2.8 midpoint: 0.8
            float temp = biome.getBaseTemperature() + 0.2f;

            if(ColdSweatCompat.LOADED)
                {
                    temp = (float) (ColdSweatCompat.bodyTemperature(player) / 100f);
                }
            else
            {
                if(temp <= 0)
                    temp = (float) Math.exp(temp);
                else if(temp > 1)
                    temp /= 2;
            }

            float thirstModifier = CommonConfig.THIRST_DEPLETION_MODIFIER.get().floatValue() * (temp  / humidity);

            if(thirstModifier < 1)
            {
                float modifierOffset = 1 - thirstModifier;
                modifierOffset *= MODIFIER_HARSHNESS;
                thirstModifier = 1 - modifierOffset;
            }

            return thirstModifier;
        }
    }
}
