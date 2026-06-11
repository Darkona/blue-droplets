package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Water containers cooking on a campfire give off vapour ({@code EFFECT} particles) instead of smoke; the other items
 * and the campfire itself keep their smoke. {@code particleTick} only runs on the client thread, so one flag, set for
 * each slot before its particles, is enough.
 */
@Mixin(CampfireBlockEntity.class)
public abstract class MixinCampfireBlockEntity
{
    @Unique
    private static boolean bluedroplets$water;

    @WrapOperation(method = "particleTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"))
    private static Object bluedroplets$checkWater(NonNullList<ItemStack> items, int slot, Operation<Object> original)
    {
        Object item = original.call(items, slot);
        bluedroplets$water = item instanceof ItemStack stack && WaterPurity.isWaterFilledContainer(stack);
        return item;
    }

    @ModifyArg(method = "particleTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"), index = 0)
    private static ParticleOptions bluedroplets$vapour(ParticleOptions particle)
    {
        return bluedroplets$water ? ParticleTypes.EFFECT : particle;
    }
}
