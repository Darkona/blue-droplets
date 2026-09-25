package com.darkona.dropletsofthirst.content.registry;

import com.darkona.dropletsofthirst.api.PurityLevel;
import com.darkona.dropletsofthirst.content.data.DropletsTags;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.foundation.config.PurityConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Water purity on items and fluids. Minecraft 1.18.2 has no data components: it is an int in the stack's NBT under
 * {@link #PURITY}, named like the {@code droplets_of_thirst:purity} component of later versions. Only
 * {@link WaterPurity} reads and writes it.
 */
public final class ThirstComponent {
    /** NBT key of the purity, {@link PurityLevel#MIN} to {@link PurityLevel#MAX}. */
    public static final String PURITY = "droplets_of_thirst:purity";

    /**
     * Key of Thirst Was Taken's purity, on its scale of four levels. Never kept: water stacks swap it for
     * {@link #PURITY} as they load ({@link #migrateLegacyPurity}).
     */
    public static final String LEGACY_PURITY = "Purity";

    /** {@code purity.defaultPurity}'s own default, for values read before the config is loaded. */
    private static final int UNLOADED_DEFAULT_PURITY = PurityLevel.ACCEPTABLE.level();

    private ThirstComponent() {
    }

    /**
     * The stored purity, or {@code null} when there is none. Out-of-range values (other mods, hand-written NBT) read
     * as the default purity instead of an invalid level, like the component's codec in later versions.
     */
    public static @Nullable Integer get(ItemStack stack)
    {
        return read(stack.getTag());
    }

    public static @Nullable Integer get(FluidStack fluid)
    {
        return read(fluid.getTag());
    }

    public static boolean has(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(PURITY, Tag.TAG_ANY_NUMERIC);
    }

    public static void set(ItemStack stack, int purity)
    {
        stack.getOrCreateTag().putInt(PURITY, bounded(purity));
    }

    public static void set(FluidStack fluid, int purity)
    {
        fluid.getOrCreateTag().putInt(PURITY, bounded(purity));
    }

    /**
     * Removes the purity; an NBT compound left empty is removed too, so the stack stacks with plain ones again.
     */
    public static void remove(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(PURITY))
            return;
        tag.remove(PURITY);
        if (tag.isEmpty())
            stack.setTag(null);
    }

    public static void remove(FluidStack fluid)
    {
        CompoundTag tag = fluid.getTag();
        if (tag == null || !tag.contains(PURITY))
            return;
        tag.remove(PURITY);
        if (tag.isEmpty())
            fluid.setTag(null);
    }

    private static @Nullable Integer read(@Nullable CompoundTag tag)
    {
        if (tag == null || !tag.contains(PURITY, Tag.TAG_ANY_NUMERIC))
            return null;
        return bounded(tag.getInt(PURITY));
    }

    /**
     * Replaces Thirst Was Taken's {@code Purity} by the same purity on the new scale, unless the stack already has a
     * {@link #PURITY}. Only water containers: {@code Purity} is a common key, and other mods' items keep theirs. One
     * NBT lookup when there is none.
     */
    public static void migrateLegacyPurity(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(LEGACY_PURITY, Tag.TAG_ANY_NUMERIC) || !isLegacyWaterContainer(stack))
            return;
        int legacy = tag.getInt(LEGACY_PURITY);
        tag.remove(LEGACY_PURITY);
        if (!tag.contains(PURITY))
            tag.putInt(PURITY, bounded(LegacyIds.purityFromLegacy(legacy)));
    }

    /**
     * {@link #migrateLegacyPurity(ItemStack)} for water and the fluids that carry purity.
     */
    public static void migrateLegacyPurity(FluidStack fluid)
    {
        CompoundTag tag = fluid.getTag();
        if (tag == null || !tag.contains(LEGACY_PURITY, Tag.TAG_ANY_NUMERIC))
            return;
        if (!fluid.getFluid().isSame(Fluids.WATER) && !fluid.getFluid().is(DropletsTags.CARRIES_PURITY))
            return;
        int legacy = tag.getInt(LEGACY_PURITY);
        tag.remove(LEGACY_PURITY);
        if (!tag.contains(PURITY))
            tag.putInt(PURITY, bounded(LegacyIds.purityFromLegacy(legacy)));
    }

    private static boolean isLegacyWaterContainer(ItemStack stack)
    {
        if (stack.is(Items.WATER_BUCKET) || stack.is(ItemInit.TERRACOTTA_WATER_BOWL.get()) || stack.is(DropletsTags.PURITY_CONTAINERS))
            return true;
        return (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))
                && PotionUtils.getPotion(stack) == Potions.WATER;
    }

    private static int bounded(int purity)
    {
        if (purity >= WaterPurity.MIN_PURITY && purity <= WaterPurity.MAX_PURITY)
            return purity;
        return PurityConfig.SPEC.isLoaded() ? WaterPurity.defaultPurity() : UNLOADED_DEFAULT_PURITY;
    }
}
