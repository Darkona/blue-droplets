package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A bucket filled with a fluid that has a purity gets that purity. {@code FluidUtil.getFilledBucket} (and with it
 * {@code FluidBucketWrapper.fill}) only skips this method for fluids without components.
 */
@Mixin(value = FluidAttributes.class, remap = false)
public abstract class MixinFluidAttributes
{
    @ModifyReturnValue(method = "getBucket(Lnet/minecraftforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private ItemStack blue_droplets$bucketPurity(ItemStack bucket, @Local(argsOnly = true) FluidStack stack)
    {
        if (!bucket.isEmpty() && WaterPurity.hasPurity(stack))
            WaterPurity.addPurity(bucket, WaterPurity.getPurity(stack));
        return bucket;
    }
}
