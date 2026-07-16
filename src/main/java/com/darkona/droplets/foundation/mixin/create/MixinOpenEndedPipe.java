package com.darkona.droplets.foundation.mixin.create;

import com.darkona.droplets.content.purity.PouredWater;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.CompatConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Water pulled from the world or from a water cauldron by an open pipe end keeps the purity it had there, as buckets
 * and the hose pulley do ({@code create.openEndedPipePurity}). The purity is read before Create drains the block,
 * because afterwards the source or the cauldron level is gone; a poured source it drains leaves its purity to the
 * sources beside it, as when a bucket picks it up. Water an open pipe end places in the world registers the source
 * as poured water of its purity.
 */
@Mixin(value = OpenEndedPipe.class, remap = false)
public abstract class MixinOpenEndedPipe
{
    @Inject(method = "removeFluidFromSpace", at = @At("HEAD"), remap = false)
    private void blue_droplets$readPurity(boolean simulate, CallbackInfoReturnable<FluidStack> cir, @Share("purity") LocalIntRef purity)
    {
        purity.set(-1);
        OpenEndedPipe pipe = (OpenEndedPipe) (Object) this;
        Level level = pipe.getWorld();
        if (level == null || !CompatConfig.OPEN_ENDED_PIPE_PURITY.get() || !WaterPurity.enabled())
            return;
        BlockPos pos = pipe.getOutputPos();
        if (!level.isLoaded(pos))
            return;
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        if (fluid.is(FluidTags.WATER))
            purity.set(WaterPurity.getWaterPurity(level, pos, fluid.isSource()));
        else if (state.is(Blocks.WATER_CAULDRON))
            purity.set(WaterPurity.cauldronPurity(level, pos));
    }

    @ModifyReturnValue(method = "removeFluidFromSpace", at = @At("RETURN"), remap = false)
    private FluidStack blue_droplets$addPurity(FluidStack drained, boolean simulate, @Share("purity") LocalIntRef purity)
    {
        if (purity.get() >= 0 && !drained.isEmpty() && drained.getFluid().is(FluidTags.WATER))
        {
            if (!WaterPurity.hasPurity(drained))
                WaterPurity.addPurity(drained, purity.get());
            OpenEndedPipe pipe = (OpenEndedPipe) (Object) this;
            if (!simulate && pipe.getWorld() != null && !pipe.getWorld().getFluidState(pipe.getOutputPos()).is(FluidTags.WATER))
                PouredWater.pickedUp(pipe.getWorld(), pipe.getOutputPos());
        }
        return drained;
    }

    @ModifyReturnValue(method = "provideFluidToSpace", at = @At("RETURN"), remap = false)
    private boolean blue_droplets$registerPouredWater(boolean placed, FluidStack fluid, boolean simulate)
    {
        OpenEndedPipe pipe = (OpenEndedPipe) (Object) this;
        if (placed && !simulate && pipe.getWorld() != null && fluid.getFluid().is(FluidTags.WATER))
            PouredWater.poured(pipe.getWorld(), pipe.getOutputPos(), WaterPurity.getPurity(fluid));
        return placed;
    }
}
