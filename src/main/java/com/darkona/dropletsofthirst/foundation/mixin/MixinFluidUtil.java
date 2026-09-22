package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.purity.PouredWater;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Water placed in the world through Forge's {@code FluidUtil.tryPlaceFluid} (fluid containers of many mods, and
 * the {@code ItemStack} overload, which ends here) registers the source it leaves as poured water of its purity.
 */
@Mixin(value = FluidUtil.class, remap = false)
public abstract class MixinFluidUtil
{
    @ModifyReturnValue(method = "tryPlaceFluid(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/BlockPos;Lnet/minecraftforge/fluids/capability/IFluidHandler;Lnet/minecraftforge/fluids/FluidStack;)Z", at = @At("RETURN"))
    private static boolean droplets_of_thirst$registerPouredWater(boolean placed, @Nullable Player player, Level level, InteractionHand hand, BlockPos pos, IFluidHandler source, FluidStack resource)
    {
        if (placed && level != null && !level.isClientSide() && resource.getFluid().is(FluidTags.WATER))
            PouredWater.poured(level, pos, WaterPurity.getPurity(resource));
        return placed;
    }
}
