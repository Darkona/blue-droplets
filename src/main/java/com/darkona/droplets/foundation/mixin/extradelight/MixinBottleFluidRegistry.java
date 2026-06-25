package com.darkona.droplets.foundation.mixin.extradelight;

import com.darkona.droplets.content.purity.WaterPurity;
import com.lance5057.extradelight.util.BottleFluidRegistry;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Extra Delight turns water bottles into 250 mB of water and back through a static table that knows nothing of
 * components, so the purity was lost both ways: a dirty bottle went into its tanks as water without a purity. The
 * fluid it returns is the table's own stack, so it is copied before the purity is set.
 */
@Mixin(value = BottleFluidRegistry.class, remap = false)
public abstract class MixinBottleFluidRegistry
{
    @ModifyReturnValue(method = "getFluidFromBottle", at = @At("RETURN"))
    private static FluidStack blue_droplets$bottlePurityToFluid(FluidStack fluid, @Local(argsOnly = true) ItemStack bottle)
    {
        if (fluid.isEmpty() || !fluid.getFluid().isSame(Fluids.WATER) || !WaterPurity.isWaterFilledContainer(bottle) || !WaterPurity.hasPurity(bottle))
            return fluid;
        return WaterPurity.addPurity(fluid.copy(), WaterPurity.getPurity(bottle));
    }

    @ModifyReturnValue(method = "getBottleFromFluid", at = @At("RETURN"))
    private static ItemStack blue_droplets$fluidPurityToBottle(ItemStack bottle, @Local(argsOnly = true) FluidStack fluid)
    {
        if (WaterPurity.hasPurity(fluid) && WaterPurity.isWaterFilledContainer(bottle))
            WaterPurity.addPurity(bottle, WaterPurity.getPurity(fluid));
        return bottle;
    }
}
