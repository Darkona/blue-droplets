package com.darkona.droplets.foundation.mixin.create;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A water container Create fills (spouts, clicking a tank or basin) gets the purity of the water it was filled with.
 */
@Mixin(value = GenericItemFilling.class, remap = false)
public class MixinGenericItemFilling
{
    @ModifyReturnValue(method = "fillItem", at = @At("RETURN"))
    private static ItemStack blue_droplets$fluidPurity(ItemStack output, Level world, int requiredAmount, ItemStack stack, FluidStack availableFluid)
    {
        if (WaterPurity.hasPurity(availableFluid) && WaterPurity.isWaterFilledContainer(output))
            WaterPurity.addPurity(output, WaterPurity.getPurity(availableFluid));
        return output;
    }
}
