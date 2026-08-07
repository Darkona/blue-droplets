package com.darkona.droplets.foundation.mixin.create;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A spout filling a water container through a filling recipe (terracotta bowl) gives it the purity of the water it
 * used; the recipe's result has none. Bottles and buckets are filled by {@code GenericItemFilling} and the fluid
 * capability, which already carry it.
 */
@Mixin(value = FillingBySpout.class, remap = false)
public class MixinFillingBySpout
{
    @ModifyReturnValue(method = "fillItem", at = @At("RETURN"))
    private static ItemStack blue_droplets$keepPurity(ItemStack filled, @Local(argsOnly = true) FluidStack availableFluid)
    {
        if (!filled.isEmpty() && WaterPurity.hasPurity(availableFluid) && !WaterPurity.hasPurity(filled) && WaterPurity.isWaterFilledContainer(filled))
            WaterPurity.addPurity(filled, WaterPurity.getPurity(availableFluid));
        return filled;
    }
}
