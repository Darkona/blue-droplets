package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fluids read from a save (tanks, Create pipes): a Thirst Was Taken purity becomes a purity on the new scale, as for
 * item stacks.
 */
@Mixin(value = FluidStack.class, remap = false)
public abstract class MixinFluidStack
{
    @ModifyReturnValue(method = "loadFluidStackFromNBT", at = @At("RETURN"))
    private static FluidStack droplets_of_thirst$migrateLegacyPurity(FluidStack fluid)
    {
        if (!fluid.isEmpty())
            ThirstComponent.migrateLegacyPurity(fluid);
        return fluid;
    }
}
