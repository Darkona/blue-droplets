package com.darkona.droplets.foundation.dev;

import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.thirst.ThirstHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * {@code /blue_droplets dev dump_items <namespace>...}, development only: one CSV row per item of those namespaces with
 * its food values, effects, tags and the thirst values Blue Droplets resolves today. Input for the data scripts.
 */
public final class ItemDump
{
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
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> namespaces.contains(id.getNamespace()))
                .sorted()
                .forEach(id -> lines.add(row(id, BuiltInRegistries.ITEM.get(id))));
        return lines;
    }

    private static String row(ResourceLocation id, Item item)
    {
        ItemStack stack = item.getDefaultInstance();
        FoodProperties food = stack.getFoodProperties(null);
        ThirstValues values = ThirstHelper.valuesOf(stack);
        boolean drink = stack.getUseAnimation() == UseAnim.DRINK || WaterPurity.isWaterFilledContainer(stack);
        String effects = food == null ? "" : food.effects().stream().map(ItemDump::effect).collect(Collectors.joining(";"));
        String tags = stack.getTags().map(TagKey::location).map(ResourceLocation::toString).sorted().collect(Collectors.joining(";"));
        return String.join(",",
                id.toString(),
                id.getNamespace(),
                String.valueOf(drink),
                food == null ? "" : String.valueOf(food.nutrition()),
                food == null ? "" : String.format(Locale.ROOT, "%.2f", food.saturation()),
                quote(effects),
                food == null ? "" : String.valueOf(food.canAlwaysEat()),
                values == null ? "" : String.valueOf(values.thirst()),
                values == null ? "" : String.valueOf(values.quenched()),
                quote(tags));
    }

    private static String effect(FoodProperties.PossibleEffect possible)
    {
        MobEffectInstance effect = possible.effect();
        String id = effect.getEffect().unwrapKey().map(key -> key.location().toString()).orElse("?");
        return id + "," + effect.getDuration() + "," + effect.getAmplifier() + "," + String.format(Locale.ROOT, "%.2f", possible.probability());
    }

    private static String quote(String field)
    {
        return field.indexOf(',') < 0 && field.indexOf('"') < 0 ? field : '"' + field.replace("\"", "\"\"") + '"';
    }
}
