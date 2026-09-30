package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.purity.PouredWater;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Water placed through the older {@code FluidUtil.tryPlaceFluid} of NeoForge, which mods written for the previous fluid
 * API still call, registers the source it leaves as poured water of its purity, as {@link MixinFluidUtil} does for the
 * transfer API.
 */
@SuppressWarnings("removal")
@Mixin(value = FluidUtil.class, remap = false)
public abstract class MixinLegacyFluidUtil
{
    @ModifyReturnValue(method = "tryPlaceFluid(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/BlockPos;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/neoforged/neoforge/fluids/FluidStack;)Z", at = @At("RETURN"))
    private static boolean droplets_of_thirst$registerPouredWater(boolean placed, @Nullable Player player, Level level, InteractionHand hand, BlockPos pos, IFluidHandler source, FluidStack resource)
    {
        if (placed && level != null && !level.isClientSide() && resource.is(FluidTags.WATER))
            PouredWater.poured(level, pos, WaterPurity.getPurity(resource));
        return placed;
    }
}
