package com.darkona.droplets.api;

import com.darkona.droplets.compat.coldsweat.ColdSweatCompat;
import com.darkona.droplets.compat.supernatural.SupernaturalCompat;
import com.darkona.droplets.content.purity.ContainerWithPurity;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.common.event.RegisterThirstValueEvent;
import com.darkona.droplets.foundation.config.CommonConfig;
import com.darkona.droplets.foundation.config.ContainerConfig;
import com.darkona.droplets.foundation.config.ItemSettingsConfig;
import com.darkona.droplets.foundation.config.KeyWordConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ThirstHelper
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float MODIFIER_HARSHNESS = 0.5f;
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private record Table(Map<Item, int[]> drinks, Map<Item, int[]> foods) {}

    private static volatile Table table = new Table(Map.of(), Map.of());

    /**
     * Rebuilds the drink/food tables and the purity containers from config, {@link RegisterThirstValueEvent}
     * and keywords. Call once tags are bound; the tables are replaced as a whole, never mutated.
     */
    public static void rebuild()
    {
        Map<Item, int[]> drinks = new HashMap<>();
        Map<Item, int[]> foods = new HashMap<>();
        List<ContainerWithPurity> containers = new ArrayList<>();

        readValues(ItemSettingsConfig.DRINKS.get(), drinks);
        readValues(ItemSettingsConfig.FOODS.get(), foods);
        for (String id : ContainerConfig.CONTAINERS.get())
            resolve(id, item -> containers.add(new ContainerWithPurity(item)));

        NeoForge.EVENT_BUS.post(new RegisterThirstValueEvent(drinks, foods, containers));

        if (KeyWordConfig.ENABLE_KEYWORD_CONFIG.get())
            addKeywordItems(drinks, foods);

        for (String id : ItemSettingsConfig.ITEMS_BLACKLIST.get())
            resolve(id, item -> {
                drinks.remove(item);
                foods.remove(item);
            });

        table = new Table(Map.copyOf(drinks), Map.copyOf(foods));
        WaterPurity.setContainers(containers);
    }

    private static void readValues(List<? extends List<?>> entries, Map<Item, int[]> target)
    {
        for (List<?> entry : entries)
        {
            if (!ItemSettingsConfig.isValidEntry(entry))
                continue;
            int[] values = {((Number) entry.get(1)).intValue(), ((Number) entry.get(2)).intValue()};
            resolve((String) entry.get(0), item -> target.put(item, values));
        }
    }

    private static void resolve(String id, Consumer<Item> sink)
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

        if (!WARNED.add(id))
            return;
        if (location != null && !ModList.get().isLoaded(location.getNamespace()))
            LOGGER.debug("Skipping config entry '{}': mod '{}' is not installed", id, location.getNamespace());
        else
            LOGGER.warn("Skipping config entry '{}': no such item or tag", id);
    }

    private static void addKeywordItems(Map<Item, int[]> drinks, Map<Item, int[]> foods)
    {
        Pattern blacklist = keyword(KeyWordConfig.KEYWORD_BLACKLIST.get());
        Pattern drink = keyword(KeyWordConfig.KEYWORD_DRINK.get());
        Pattern soup = keyword(KeyWordConfig.KEYWORD_SOUP.get());
        Pattern fruit = keyword(KeyWordConfig.KEYWORD_FRUIT.get());
        int[] drinkValues = {KeyWordConfig.getDrinkHydration(), KeyWordConfig.getDrinkQuenchness()};
        int[] soupValues = {KeyWordConfig.getSoupHydration(), KeyWordConfig.getSoupQuenchness()};
        int[] fruitValues = {KeyWordConfig.getFruitHydration(), KeyWordConfig.getFruitQuenchness()};

        for (Item item : BuiltInRegistries.ITEM)
        {
            if (drinks.containsKey(item) || foods.containsKey(item) || item.getDefaultInstance().getFoodProperties(null) != null)
                continue;

            String name = item.getDescriptionId();
            if (matches(blacklist, name))
                continue;
            if (matches(drink, name))
                drinks.put(item, drinkValues);
            else if (matches(soup, name))
                foods.put(item, soupValues);
            else if (matches(fruit, name))
                foods.put(item, fruitValues);
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
        final float perLevelMultiplier = 0.0625f;
        float totalLevels = EnchantmentHelper.getDamageProtection((ServerLevel) player.level(),player, player.damageSources().onFire()) / 2;
        //In some situations, the player can have more than 12 levels of fire protection due to some bugs
        if(totalLevels>12) totalLevels=12;
        return 1.0f - ((totalLevels * perLevelMultiplier) * 0.75f);
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
