package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;
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
    private FluidStack droplets_of_thirst$bucketPurity(FluidStack fluid)
    {
        if (!fluid.isEmpty() && WaterPurity.hasPurity(container))
            WaterPurity.addPurity(fluid, WaterPurity.getPurity(container));
        return fluid;
    }
}
