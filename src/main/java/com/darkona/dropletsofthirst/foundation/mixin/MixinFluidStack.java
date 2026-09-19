package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fluids read from a save or the network (tanks, Create pipes): a Thirst Was Taken purity becomes a purity on the new
 * scale, as for item stacks.
 */
@Mixin(FluidStack.class)
public abstract class MixinFluidStack
{
    @Inject(method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentPatch;)V", at = @At("TAIL"))
    private void droplets_of_thirst$migrateLegacyPurity(Holder<Fluid> fluid, int amount, DataComponentPatch patch, CallbackInfo ci)
    {
        if (!patch.isEmpty())
            ThirstComponent.migrateLegacyPurity((FluidStack) (Object) this);
    }
}
