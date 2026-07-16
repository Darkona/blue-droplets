package com.darkona.droplets.foundation.mixin.extradelight;

import com.darkona.droplets.compat.delight.DelightCompat;
import com.darkona.droplets.compat.delight.WaterSourceTank;
import com.lance5057.extradelight.capabilities.WellFluidCapability;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The infinite water of Extra Delight's taps and sinks gets the purity of the world's water where they stand
 * ({@code delight.worldPurityWaterSources}). The tank does not know its block, so the tap or sink tells it
 * ({@link MixinWaterSourceBlockEntities}). {@code getFluid} too, so pipes that match what they see with what they
 * drain get the same water.
 */
@Mixin(value = WellFluidCapability.class, remap = false)
public abstract class MixinWellFluidCapability implements WaterSourceTank
{
    @Unique
    private BlockEntity blue_droplets$source;

    @Override
    public void blue_droplets$setSource(BlockEntity source)
    {
        blue_droplets$source = source;
    }

    @ModifyReturnValue(method = {"getFluid", "drain(ILnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;"}, at = @At("RETURN"))
    private FluidStack blue_droplets$worldPurity(FluidStack water)
    {
        return DelightCompat.withSourcePurity(water, blue_droplets$source);
    }
}
