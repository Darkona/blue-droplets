package com.darkona.droplets.content;

import com.darkona.droplets.api.DrinkValueProvider;
import com.darkona.droplets.api.DropletsView;
import com.darkona.droplets.api.ExhaustionModifier;
import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.api.event.ThirstChangeEvent;
import com.darkona.droplets.api.spi.DropletsService;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.thirst.ExhaustionFactors;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.gui.ThirstBarStyles;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * What {@link com.darkona.droplets.api.DropletsAPI} calls; set in the mod constructor.
 */
public final class DropletsServiceImpl implements DropletsService
{
    public static final DropletsServiceImpl INSTANCE = new DropletsServiceImpl();

    private static boolean warnedClientChange;

    private DropletsServiceImpl() {}

    private static @Nullable PlayerThirst server(Player player)
    {
        if (!player.level.isClientSide)
            return ModAttachment.thirst(player);
        if (!warnedClientChange)
        {
            warnedClientChange = true;
            LogUtils.getLogger().warn("A mod tried to change thirst on the client through DropletsAPI; only the server can", new IllegalStateException());
        }
        return null;
    }

    @Override
    public DropletsView view(Player player)
    {
        return ModAttachment.thirst(player);
    }

    @Override
    public @Nullable ThirstValues getDrinkValues(ItemStack stack)
    {
        return ThirstHelper.drinkValuesOf(stack);
    }

    @Override
    public boolean setThirst(Player player, int thirst)
    {
        PlayerThirst data = server(player);
        return data != null && data.change(player, thirst, data.getQuenched(), ThirstChangeEvent.Cause.API);
    }

    @Override
    public boolean setQuenched(Player player, int quenched)
    {
        PlayerThirst data = server(player);
        return data != null && data.change(player, data.getThirst(), quenched, ThirstChangeEvent.Cause.API);
    }

    @Override
    public boolean addThirst(Player player, int thirst, int quenched)
    {
        PlayerThirst data = server(player);
        return data != null && data.change(player, data.getThirst() + thirst, data.getQuenched() + quenched, ThirstChangeEvent.Cause.API);
    }

    @Override
    public boolean drink(Player player, int thirst, int quenched, int purity)
    {
        return server(player) != null && PlayerThirst.drink(player, ItemStack.EMPTY, thirst, quenched, purity);
    }

    @Override
    public boolean eat(Player player, int thirst, int quenched)
    {
        return server(player) != null && PlayerThirst.eat(player, ItemStack.EMPTY, thirst, quenched);
    }

    @Override
    public void addExhaustion(Player player, float amount)
    {
        PlayerThirst data = server(player);
        if (data != null)
            data.addScaledExhaustion(player, amount);
    }

    @Override
    public boolean isPurityEnabled()
    {
        return WaterPurity.enabled();
    }

    @Override
    public int getPurity(ItemStack stack)
    {
        return WaterPurity.getPurity(stack);
    }

    @Override
    public int getPurity(FluidStack fluid)
    {
        return WaterPurity.getPurity(fluid);
    }

    @Override
    public ItemStack withPurity(ItemStack stack, int purity)
    {
        return WaterPurity.addPurity(stack.copy(), purity);
    }

    @Override
    public FluidStack withPurity(FluidStack fluid, int purity)
    {
        return WaterPurity.addPurity(fluid.copy(), purity);
    }

    @Override
    public int getWaterPurity(Level level, BlockPos pos)
    {
        return WaterPurity.getBlockPurity(level, pos);
    }

    @Override
    public void registerDrink(ItemLike item, int thirst, int quenched, int purity)
    {
        ThirstHelper.registerDrink(item, thirst, quenched, purity);
    }

    @Override
    public void registerDrinkProvider(ItemLike item, DrinkValueProvider provider)
    {
        ThirstHelper.registerProvider(item, provider);
    }

    @Override
    public void registerContainer(@Nullable ItemLike empty, ItemLike filled)
    {
        ThirstHelper.registerContainer(empty, filled);
    }

    @Override
    public void registerExhaustionModifier(ResourceLocation id, ExhaustionModifier modifier)
    {
        ExhaustionFactors.register(id, modifier);
    }

    @Override
    public void refreshExhaustionModifier(Player player)
    {
        ModAttachment.thirst(player).invalidateModifier();
    }

    @Override
    public void registerBarStyle(ResourceLocation id, Predicate<Player> active, int rgb, int priority)
    {
        int color = rgb & 0xFFFFFF;
        ThirstBarStyles.register(id, active, () -> color, priority);
    }

    @Override
    public void registerWaveEffect(MobEffect effect)
    {
        ThirstBarStyles.registerWave(effect);
    }
}
