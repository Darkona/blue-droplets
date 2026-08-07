package com.darkona.droplets.foundation.mixin.extradelight;

import com.darkona.droplets.compat.delight.WaterSourceTank;
import com.lance5057.extradelight.blocks.entities.TapBlockEntity;
import com.lance5057.extradelight.blocks.sink.SinkCabinetBlockEntity;
import com.lance5057.extradelight.capabilities.WellFluidCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Taps and sinks tell their infinite water tank which block it belongs to (see {@link MixinWellFluidCapability}).
 */
@Mixin(value = {TapBlockEntity.class, SinkCabinetBlockEntity.class}, remap = false)
public abstract class MixinWaterSourceBlockEntities
{
    @Shadow(remap = false)
    WellFluidCapability water;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void blue_droplets$tellTank(BlockPos pos, BlockState state, CallbackInfo ci)
    {
        ((WaterSourceTank) water).blue_droplets$setSource((BlockEntity) (Object) this);
    }
}
