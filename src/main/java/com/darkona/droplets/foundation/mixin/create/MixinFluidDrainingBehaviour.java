package com.darkona.droplets.foundation.mixin.create;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Water a hose pulley drains from the world gets the purity of the block it is drained from.
 */
@Mixin(value = FluidDrainingBehaviour.class, remap = false)
public abstract class MixinFluidDrainingBehaviour
{
    @ModifyReturnValue(method = "getDrainableFluid", at = @At("RETURN"), remap = false)
    private FluidStack blue_droplets$blockPurity(FluidStack output, BlockPos rootPos)
    {
        if (WaterPurity.enabled() && FluidHelper.isWater(output.getFluid()))
            WaterPurity.addPurity(output, WaterPurity.getBlockPurity(((FluidDrainingBehaviour) (Object) this).getWorld(), rootPos));
        return output;
    }
}
