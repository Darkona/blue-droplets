package com.darkona.droplets.foundation.config;

import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.core.NumberRows;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Problems NeoForge's per-key validation cannot see (unknown ids, overlapping bands, unordered curves, bad patterns,
 * recipes the inference could not read).
 * Reported as one warning each time the tables are rebuilt (world load, {@code /reload}, config file change) and by
 * {@code /bluedroplets config check}. Nothing here stops loading: bad entries are skipped where they are used.
 */
public final class ConfigCheck
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private ConfigCheck() {}

    public static List<String> problems()
    {
        List<String> problems = new ArrayList<>();
        overlaps("purity.toml world.altitudeBands", PurityConfig.ALTITUDE_BANDS, problems);
        overlaps("gameplay.toml depletion.climate.altitude", GameplayConfig.ALTITUDE_MULTIPLIERS, problems);
        curve("gameplay.toml depletion.climate.temperatureCurve", GameplayConfig.TEMPERATURE_CURVE, problems);
        curve("gameplay.toml depletion.climate.humidityCurve", GameplayConfig.HUMIDITY_CURVE, problems);
        curve("compat.toml coldsweat.bodyTemperatureCurve", CompatConfig.COLD_SWEAT_BODY_TEMPERATURE_CURVE, problems);
        for (int purity = 0; purity < PurityConfig.EFFECTS.size(); purity++)
            for (String entry : PurityConfig.EFFECTS.get(purity).get())
            {
                ResourceLocation id = ResourceLocation.tryParse(entry.split(",")[0].trim());
                if (id != null && !BuiltInRegistries.MOB_EFFECT.containsKey(id))
                    problems.add("purity.toml effects." + PurityConfig.EFFECT_LEVELS[purity] + ": unknown effect " + id + " (skipped)");
            }
        if (ItemsConfig.KEYWORDS.get())
            for (ModConfigSpec.ConfigValue<String> keyword : List.of(ItemsConfig.KEYWORD_BLACKLIST, ItemsConfig.KEYWORD_DRINK, ItemsConfig.KEYWORD_SOUP, ItemsConfig.KEYWORD_FRUIT))
                pattern(keyword, problems);
        if (GameplayConfig.SLOW_REGEN_MIN_THIRST.get() > GameplayConfig.FULL_REGEN_MIN_THIRST.get())
            problems.add("gameplay.toml regeneration.slowRegenMinThirst is above fullRegenMinThirst (slow regeneration never happens)");
        List<String> unknown = ThirstHelper.unknownConfigIds();
        if (!unknown.isEmpty())
            problems.add("items.toml: " + unknown.size() + " entries with no such item or tag (skipped): " + unknown);
        problems.addAll(ThirstHelper.inferenceProblems());
        return problems;
    }

    public static void report()
    {
        List<String> problems = problems();
        if (!problems.isEmpty())
            LOGGER.warn("Blue Droplets config has {} problem(s):\n  {}", problems.size(), String.join("\n  ", problems));
    }

    private static void overlaps(String key, ModConfigSpec.ConfigValue<List<? extends String>> value, List<String> problems)
    {
        List<? extends String> rows = value.get();
        for (int i = 0; i < rows.size(); i++)
        {
            double[] a = NumberRows.parseRow(rows.get(i), 3);
            for (int j = i + 1; a != null && j < rows.size(); j++)
            {
                double[] b = NumberRows.parseRow(rows.get(j), 3);
                if (b != null && a[0] <= b[1] && b[0] <= a[1])
                    problems.add(key + ": \"" + rows.get(i) + "\" and \"" + rows.get(j) + "\" overlap (the first one wins)");
            }
        }
    }

    private static void curve(String key, ModConfigSpec.ConfigValue<List<? extends String>> value, List<String> problems)
    {
        if (!NumberRows.ascending(NumberRows.parse(value.get(), 2)))
            problems.add(key + ": points are not in ascending order " + value.get());
    }

    private static void pattern(ModConfigSpec.ConfigValue<String> value, List<String> problems)
    {
        try
        {
            Pattern.compile(value.get());
        }
        catch (PatternSyntaxException e)
        {
            problems.add("items.toml keywords." + value.getPath().getLast() + ": invalid pattern (" + e.getDescription() + ")");
        }
    }
}
