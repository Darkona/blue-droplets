package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The fluid of a bucket carries the bucket's purity: every read goes through {@code getFluid} (tank contents, both
 * {@code drain} overloads).
 */
@Mixin(value = FluidBucketWrapper.class, remap = false)
public abstract class MixinFluidBucketWrapper
{
    @Shadow protected ItemStack container;

    @ModifyReturnValue(method = "getFluid", at = @At("RETURN"))
    private FluidStack blue_droplets$bucketPurity(FluidStack fluid)
    {
        if (!fluid.isEmpty() && WaterPurity.hasPurity(container))
            WaterPurity.addPurity(fluid, WaterPurity.getPurity(container));
        return fluid;
    }
}
