package com.darkona.droplets.compat.vampirism;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.foundation.config.CompatConfig;
import com.darkona.droplets.foundation.gui.ThirstBarStyles;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Vampirism: its vampires get the red bar and lose thirst like anyone; only blood hydrates them. Every drink of blood
 * (bite, bottle, container, blood food) arrives through Vampirism's {@code PlayerDrinkBloodEvent} and restores thirst
 * by {@code compat.toml} {@code vampirism.*}. Its blood items are not in {@code blue_droplets:drinks}: that would count
 * them twice.
 */
public final class VampirismCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("vampirism");

    private VampirismCompat() {}

    public static void init()
    {
        if (LOADED)
            VampirismBridge.init();
    }

    public static void initClient()
    {
        if (LOADED)
            ThirstBarStyles.register(BlueDroplets.asResource("vampirism_vampire"), VampirismBridge::isVampire, ThirstBarStyles.VAMPIRE_COLOR, ThirstBarStyles.VAMPIRE_PRIORITY);
    }

    public static boolean isVampire(Player player)
    {
        return LOADED && VampirismBridge.isVampire(player);
    }

    /** {@code blood} points drunk from {@code source} (empty for a bite or a block) with Vampirism's saturation modifier. */
    static void bloodDrunk(Player player, ItemStack source, int blood, float saturation)
    {
        PlayerThirst.drinkBlood(player, source, (float) (blood * CompatConfig.VAMPIRISM_THIRST_PER_BLOOD.get()),
                (float) (blood * saturation * CompatConfig.VAMPIRISM_QUENCHED_PER_BLOOD.get()));
    }
}
