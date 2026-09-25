package com.darkona.dropletsofthirst.compat.vampirism;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
import com.darkona.dropletsofthirst.foundation.gui.ThirstBarStyles;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * Vampirism: its vampires get the red bar and lose thirst like anyone; only blood hydrates them. This Vampirism has no
 * blood drinking event, so every drink of blood (bite, bottle, container, blood food) arrives through a mixin at the
 * head of {@code VampirePlayer#drinkBlood} ({@code foundation.mixin.vampirism}) and restores thirst by
 * {@code compat.toml} {@code vampirism.*}.
 */
public final class VampirismCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("vampirism");

    private VampirismCompat() {}

    public static void initClient()
    {
        if (LOADED)
            ThirstBarStyles.register(DropletsOfThirst.asResource("vampirism_vampire"), VampirismBridge::isVampire, ThirstBarStyles.VAMPIRE_COLOR, ThirstBarStyles.VAMPIRE_PRIORITY);
    }

    public static boolean isVampire(Player player)
    {
        return LOADED && VampirismBridge.isVampire(player);
    }

    /**
     * {@code blood} points a vampire player drinks, with Vampirism's saturation modifier. The altars and the blood bar
     * command fill the bar with {@code Integer.MAX_VALUE}: that is not drinking.
     */
    public static void bloodDrunk(Player player, int blood, float saturation)
    {
        if (blood <= 0 || blood == Integer.MAX_VALUE || player.level.isClientSide)
            return;
        PlayerThirst.drinkBlood(player, ItemStack.EMPTY, (float) (blood * CompatConfig.VAMPIRISM_THIRST_PER_BLOOD.get()),
                (float) (blood * saturation * CompatConfig.VAMPIRISM_QUENCHED_PER_BLOOD.get()));
    }
}
