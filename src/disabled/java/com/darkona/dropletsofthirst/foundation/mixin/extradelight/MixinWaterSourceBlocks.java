package com.darkona.dropletsofthirst.foundation.mixin.extradelight;

import com.darkona.dropletsofthirst.compat.delight.DelightCompat;
import com.lance5057.extradelight.blocks.TapBlock;
import com.lance5057.extradelight.blocks.sink.SinkCabinetBlock;
import com.lance5057.extradelight.util.BottleFluidRegistry;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A glass bottle used on a tap or a sink is filled from a new water stack, not from its tank: that water gets the
 * purity of the world there too, and {@link MixinBottleFluidRegistry} passes it to the bottle.
 */
@Mixin(value = {TapBlock.class, SinkCabinetBlock.class}, remap = false)
public abstract class MixinWaterSourceBlocks
{
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/lance5057/extradelight/util/BottleFluidRegistry;getBottleFromFluid(Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack droplets_of_thirst$bottleWithWorldPurity(FluidStack water, Operation<ItemStack> original, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos)
    {
        return original.call(DelightCompat.withSourcePurity(water, level, pos));
    }
}
