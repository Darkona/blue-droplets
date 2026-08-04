package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.fluid.CauldronWrapper;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Water in a cauldron seen through the fluid capability (pumps and pipes of other mods) carries the cauldron purity,
 * as buckets and bottles do: the resource it reports is water of that purity, and extracting that resource is
 * accepted; NeoForge only accepts resources without components. The wrapper keeps its level and position in a
 * private record, so they are kept here as well when {@code get} hands the wrapper out.
 */
@Mixin(value = CauldronWrapper.class, remap = false)
public abstract class MixinCauldronWrapper
{
    @Unique
    private Level blue_droplets$level;
    @Unique
    private BlockPos blue_droplets$pos;

    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private static CauldronWrapper blue_droplets$remember(CauldronWrapper wrapper, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos)
    {
        MixinCauldronWrapper self = (MixinCauldronWrapper) (Object) wrapper;
        if (self.blue_droplets$pos == null)
        {
            self.blue_droplets$level = level;
            self.blue_droplets$pos = pos.immutable();
        }
        return wrapper;
    }

    @ModifyReturnValue(method = "getResource", at = @At("RETURN"))
    private FluidResource blue_droplets$tankPurity(FluidResource resource)
    {
        if (blue_droplets$pos == null || resource.isEmpty() || !resource.is(FluidTags.WATER) || !WaterPurity.enabled())
            return resource;
        return WaterPurity.withPurity(resource, WaterPurity.cauldronPurity(blue_droplets$level, blue_droplets$pos));
    }

    @WrapOperation(method = "extract", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/transfer/fluid/FluidResource;isComponentsPatchEmpty()Z"))
    private boolean blue_droplets$acceptOwnPurity(FluidResource resource, Operation<Boolean> original)
    {
        return original.call(resource) || blue_droplets$pos != null && resource.is(FluidTags.WATER)
                && resource.equals(WaterPurity.waterResource(WaterPurity.cauldronPurity(blue_droplets$level, blue_droplets$pos)));
    }
}
