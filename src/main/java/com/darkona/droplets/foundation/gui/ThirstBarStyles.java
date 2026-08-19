package com.darkona.droplets.foundation.gui;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.registry.EffectInit;
import com.darkona.droplets.foundation.config.ClientConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * Colours of the thirst bar while a status applies (the active style with the highest priority wins) and the effects
 * that make it wave.
 */
public final class ThirstBarStyles
{
    public static final int VAMPIRE_PRIORITY = 400;
    public static final int DEHYDRATION_PRIORITY = 300;
    public static final int OVERHYDRATED_PRIORITY = 250;
    public static final int POISON_PRIORITY = 200;
    public static final int QUENCHNESS_PRIORITY = 100;
    public static final int HYDRATED_PRIORITY = 50;
    public static final IntSupplier VAMPIRE_COLOR = color(ClientConfig.VAMPIRE_COLOR);

    private record Style(ResourceLocation id, Predicate<Player> active, IntSupplier rgb, int priority) {}

    private static final Comparator<Style> ORDER = Comparator.comparingInt(Style::priority).reversed().thenComparing(Style::id);
    private static volatile Style[] styles = {};
    private static final List<Holder<MobEffect>> waveEffects = new CopyOnWriteArrayList<>();

    private ThirstBarStyles() {}

    /** Adds or replaces the style with this id. */
    public static synchronized void register(ResourceLocation id, Predicate<Player> active, IntSupplier rgb, int priority)
    {
        List<Style> list = new ArrayList<>(List.of(styles));
        list.removeIf(style -> style.id.equals(id));
        list.add(new Style(id, active, rgb, priority));
        list.sort(ORDER);
        styles = list.toArray(Style[]::new);
    }

    /** {@code 0xRRGGBB} of the winning active style, or -1 for the normal bar. */
    public static int resolve(Player player)
    {
        for (Style style : styles)
            if (style.active.test(player))
                return style.rgb.getAsInt();
        return -1;
    }

    /** Id of the winning active style, or null for the normal bar. */
    public static @Nullable ResourceLocation activeStyle(Player player)
    {
        for (Style style : styles)
            if (style.active.test(player))
                return style.id;
        return null;
    }

    /** Effects that make the droplets bounce one at a time, like hearts under Regeneration. */
    public static void registerWave(Holder<MobEffect> effect)
    {
        waveEffects.add(effect);
    }

    public static boolean waves(Player player)
    {
        for (int i = 0; i < waveEffects.size(); i++)
            if (player.hasEffect(waveEffects.get(i)))
                return true;
        return false;
    }

    public static void registerBuiltIns()
    {
        registerWave(EffectInit.QUENCHNESS);
        registerWave(EffectInit.HYDRATED);
        register(BlueDroplets.asResource("dehydration"), player -> player.hasEffect(EffectInit.DEHYDRATION), color(ClientConfig.DEHYDRATION_COLOR), DEHYDRATION_PRIORITY);
        register(BlueDroplets.asResource("overhydrated"), player -> player.hasEffect(EffectInit.OVERHYDRATED), color(ClientConfig.OVERHYDRATED_COLOR), OVERHYDRATED_PRIORITY);
        register(ResourceLocation.withDefaultNamespace("poison"), player -> player.hasEffect(MobEffects.POISON), color(ClientConfig.POISON_COLOR), POISON_PRIORITY);
        register(BlueDroplets.asResource("quenchness"), player -> player.hasEffect(EffectInit.QUENCHNESS), color(ClientConfig.QUENCHNESS_COLOR), QUENCHNESS_PRIORITY);
        register(BlueDroplets.asResource("hydrated"), player -> player.hasEffect(EffectInit.HYDRATED), color(ClientConfig.HYDRATED_COLOR), HYDRATED_PRIORITY);
    }

    private static IntSupplier color(ModConfigSpec.ConfigValue<String> value)
    {
        return new IntSupplier()
        {
            private String parsed;
            private int rgb;

            @Override
            public int getAsInt()
            {
                String hex = value.get();
                if (hex != parsed)
                {
                    rgb = Integer.parseInt(hex, 1, 7, 16);
                    parsed = hex;
                }
                return rgb;
            }
        };
    }
}
