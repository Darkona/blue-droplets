package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A bucket filled with a fluid that has a purity gets that purity. {@code FluidUtil.getFilledBucket} (and with it
 * {@code FluidBucketWrapper.fill}) only skips this method for fluids without components.
 */
@Mixin(value = FluidType.class, remap = false)
public abstract class MixinFluidType
{
    @ModifyReturnValue(method = "getBucket(Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private ItemStack blue_droplets$bucketPurity(ItemStack bucket, @Local(argsOnly = true) FluidStack stack)
    {
        if (!bucket.isEmpty() && WaterPurity.hasPurity(stack))
            WaterPurity.addPurity(bucket, WaterPurity.getPurity(stack));
        return bucket;
    }
}
