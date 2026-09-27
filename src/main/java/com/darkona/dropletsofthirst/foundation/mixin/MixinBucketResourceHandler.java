package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.neoforged.neoforge.transfer.fluid.BucketResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The fluid of a bucket carries the bucket's purity: every read of the bucket's contents goes through
 * {@code getResourceFrom}. Filling goes through {@code FluidType.getBucket} ({@link MixinFluidType}).
 */
@Mixin(value = BucketResourceHandler.class, remap = false)
public abstract class MixinBucketResourceHandler
{
    @ModifyReturnValue(method = "getResourceFrom", at = @At("RETURN"))
    private FluidResource droplets_of_thirst$bucketPurity(FluidResource fluid, @Local(argsOnly = true) ItemResource bucket)
    {
        Integer purity = bucket.get(ThirstComponent.PURITY);
        if (purity == null || fluid.isEmpty() || !WaterPurity.enabled())
            return fluid;
        return WaterPurity.withPurity(fluid, purity);
    }
}
