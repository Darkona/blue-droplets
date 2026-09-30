package com.darkona.dropletsofthirst.foundation.dev;

import com.darkona.dropletsofthirst.api.ThirstValues;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.neoforged.fml.loading.FMLPaths;

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
import java.util.stream.Stream;

/**
 * {@code /droplets_of_thirst dev dump_items <namespace>...}, development only: one CSV row per item of those namespaces
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
        BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> wanted.contains(id.getNamespace()))
                .sorted()
                .forEach(id -> lines.add(row(id, BuiltInRegistries.ITEM.getValue(id))));
        return lines;
    }

    /**
     * Namespaces with at least one item that is eaten or drunk, or that Droplets of Thirst gives thirst values.
     */
    private static Set<String> consumableNamespaces()
    {
        Set<String> namespaces = new HashSet<>();
        for (Identifier id : BuiltInRegistries.ITEM.keySet())
        {
            ItemStack stack = BuiltInRegistries.ITEM.getValue(id).getDefaultInstance();
            if (stack.has(DataComponents.FOOD) || stack.getUseAnimation() == ItemUseAnimation.DRINK || WaterPurity.isWaterFilledContainer(stack) || ThirstHelper.valuesOf(stack) != null)
                namespaces.add(id.getNamespace());
        }
        return namespaces;
    }

    private static String row(Identifier id, Item item)
    {
        ItemStack stack = item.getDefaultInstance();
        FoodProperties food = stack.get(DataComponents.FOOD);
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        ThirstValues values = ThirstHelper.valuesOf(stack);
        boolean drink = stack.getUseAnimation() == ItemUseAnimation.DRINK || WaterPurity.isWaterFilledContainer(stack);
        String effects = food == null || consumable == null ? "" : consumable.onConsumeEffects().stream().flatMap(ItemDump::effects).collect(Collectors.joining(";"));
        String tags = stack.getTags().map(TagKey::location).map(Identifier::toString).sorted().collect(Collectors.joining(";"));
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

    /** Effects of eating: on 1.21.11 they are the food's consume effects that apply status effects. */
    private static Stream<String> effects(ConsumeEffect consumeEffect)
    {
        if (!(consumeEffect instanceof ApplyStatusEffectsConsumeEffect apply))
            return Stream.empty();
        return apply.effects().stream().map(effect -> effect(effect, apply.probability()));
    }

    private static String effect(MobEffectInstance effect, float probability)
    {
        String id = effect.getEffect().unwrapKey().map(key -> key.identifier().toString()).orElse("?");
        return id + "," + effect.getDuration() + "," + effect.getAmplifier() + "," + String.format(Locale.ROOT, "%.2f", probability);
    }

    private static String quote(String field)
    {
        return field.indexOf(',') < 0 && field.indexOf('"') < 0 ? field : '"' + field.replace("\"", "\"\"") + '"';
    }
}
