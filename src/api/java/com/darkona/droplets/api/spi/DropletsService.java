package com.darkona.droplets.api.spi;

import com.darkona.droplets.api.DropletsAPI;
import com.darkona.droplets.api.DrinkValueProvider;
import com.darkona.droplets.api.DropletsView;
import com.darkona.droplets.api.ExhaustionModifier;
import com.darkona.droplets.api.ThirstValues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Implemented by Blue Droplets. Use {@link DropletsAPI}; this interface is not meant for other mods to call or
 * implement and may change between versions.
 */
public interface DropletsService
{
    DropletsView view(Player player);

    @Nullable ThirstValues getDrinkValues(ItemStack stack);

    boolean setThirst(Player player, int thirst);

    boolean setQuenched(Player player, int quenched);

    boolean addThirst(Player player, int thirst, int quenched);

    boolean drink(Player player, int thirst, int quenched, int purity);

    void addExhaustion(Player player, float amount);

    boolean isPurityEnabled();

    int getPurity(ItemStack stack);

    int getPurity(FluidStack fluid);

    ItemStack withPurity(ItemStack stack, int purity);

    FluidStack withPurity(FluidStack fluid, int purity);

    int getWaterPurity(Level level, BlockPos pos);

    void registerDrink(ItemLike item, int thirst, int quenched, int purity);

    void registerDrinkProvider(ItemLike item, DrinkValueProvider provider);

    void registerContainer(@Nullable ItemLike empty, ItemLike filled);

    void registerExhaustionModifier(ResourceLocation id, ExhaustionModifier modifier);

    void refreshExhaustionModifier(Player player);

    void registerBarStyle(ResourceLocation id, Predicate<Player> active, int rgb, int priority);

    void registerWaveEffect(Holder<MobEffect> effect);
}
