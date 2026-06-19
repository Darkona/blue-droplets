package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dispensers hand out what they filled through {@code consumeWithRemainder}: the vanilla bucket and glass bottle, and
 * NeoForge's {@code DispenseFluidContainer} for other containers. A water container that comes out without a purity
 * gets the purity of the water in front of the dispenser. Containers filled from a tank already carry one.
 */
@Mixin(DefaultDispenseItemBehavior.class)
public abstract class MixinDefaultDispenseItemBehavior
{
    @Inject(method = "consumeWithRemainder", at = @At("HEAD"))
    private void blue_droplets$worldWaterPurity(BlockSource source, ItemStack stack, ItemStack remainder, CallbackInfoReturnable<ItemStack> cir)
    {
        if (WaterPurity.enabled() && source.state().hasProperty(DispenserBlock.FACING) && WaterPurity.isWaterFilledContainer(remainder) && !WaterPurity.hasPurity(remainder))
            WaterPurity.addPurity(remainder, WaterPurity.takenWaterPurity(source.level(), source.pos().relative(source.state().getValue(DispenserBlock.FACING))));
    }
}
