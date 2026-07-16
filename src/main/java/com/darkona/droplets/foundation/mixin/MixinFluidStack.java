package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.registry.ThirstComponent;
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
    private static FluidStack blue_droplets$migrateLegacyPurity(FluidStack fluid)
    {
        if (!fluid.isEmpty())
            ThirstComponent.migrateLegacyPurity(fluid);
        return fluid;
    }
}
