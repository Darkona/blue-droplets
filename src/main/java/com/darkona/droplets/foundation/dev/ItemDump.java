package com.darkona.droplets.foundation.dev;

import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code /blue_droplets dev dump_items <namespace>...}, development only: one CSV row per item of those namespaces
 * ({@code *}: every namespace with food or drinks) with its food values, effects, tags and the thirst values Blue
 * Droplets resolves today. Input for the data scripts.
 */
public final class ItemDump
{
    /** Namespace argument for every namespace with food or drinks. */
    public static final String ALL = "*";
    public static final String HEADER = "id,mod,is_drink,nutrition,saturation,effects,always_edible,current_thirst,current_quenched,tags";

    private ItemDump() {}

    /**
     * Writes {@code <game dir>/delight/items.csv}; returns the rows written.
     */
    public static int dump(Collection<String> namespaces) throws IOException
    {
        List<String> lines = rows(namespaces);
        Path file = file();
        Files.createDirectories(file.getParent());
        Files.write(file, lines, StandardCharsets.UTF_8);
        return lines.size() - 1;
    }

    public static Path file()
    {
        return FMLPaths.GAMEDIR.get().resolve("delight").resolve("items.csv");
    }

    /**
     * Header and rows, sorted by id.
     */
    public static List<String> rows(Collection<String> namespaces)
    {
        Set<String> wanted = namespaces.contains(ALL) ? consumableNamespaces() : Set.copyOf(namespaces);
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        Registry.ITEM.keySet().stream()
                .filter(id -> wanted.contains(id.getNamespace()))
                .sorted()
                .forEach(id -> lines.add(row(id, Registry.ITEM.get(id))));
        return lines;
    }

    /**
     * Namespaces with at least one item that is eaten or drunk, or that Blue Droplets gives thirst values.
     */
    private static Set<String> consumableNamespaces()
    {
        Set<String> namespaces = new HashSet<>();
        for (ResourceLocation id : Registry.ITEM.keySet())
        {
            ItemStack stack = Registry.ITEM.get(id).getDefaultInstance();
            if (stack.getFoodProperties(null) != null || stack.getUseAnimation() == UseAnim.DRINK || WaterPurity.isWaterFilledContainer(stack) || ThirstHelper.valuesOf(stack) != null)
                namespaces.add(id.getNamespace());
        }
        return namespaces;
    }

    private static String row(ResourceLocation id, Item item)
    {
        ItemStack stack = item.getDefaultInstance();
        FoodProperties food = stack.getFoodProperties(null);
        ThirstValues values = ThirstHelper.valuesOf(stack);
        boolean drink = stack.getUseAnimation() == UseAnim.DRINK || WaterPurity.isWaterFilledContainer(stack);
        String effects = food == null ? "" : food.getEffects().stream().map(ItemDump::effect).collect(Collectors.joining(";"));
        String tags = stack.getTags().map(TagKey::location).map(ResourceLocation::toString).sorted().collect(Collectors.joining(";"));
        return String.join(",",
                id.toString(),
                id.getNamespace(),
                String.valueOf(drink),
                food == null ? "" : String.valueOf(food.getNutrition()),
                food == null ? "" : String.format(Locale.ROOT, "%.2f", food.getNutrition() * food.getSaturationModifier() * 2.0F),
                quote(effects),
                food == null ? "" : String.valueOf(food.canAlwaysEat()),
                values == null ? "" : String.valueOf(values.thirst()),
                values == null ? "" : String.valueOf(values.quenched()),
                quote(tags));
    }

    /**
     * The saturation column is the saturation a bite gives (nutrition x modifier x 2), as later versions store it.
     */
    private static String effect(Pair<MobEffectInstance, Float> possible)
    {
        MobEffectInstance effect = possible.getFirst();
        ResourceLocation key = Registry.MOB_EFFECT.getKey(effect.getEffect());
        String id = key == null ? "?" : key.toString();
        return id + "," + effect.getDuration() + "," + effect.getAmplifier() + "," + String.format(Locale.ROOT, "%.2f", possible.getSecond());
    }

    private static String quote(String field)
    {
        return field.indexOf(',') < 0 && field.indexOf('"') < 0 ? field : '"' + field.replace("\"", "\"\"") + '"';
    }
}
