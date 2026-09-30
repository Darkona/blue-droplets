package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.purity.PouredWater;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Water placed in the world through NeoForge's {@code FluidUtil.tryPlaceFluid} (fluid containers of many mods,
 * NeoForge's dispenser behaviour for them; every overload ends here) registers the source it leaves as poured water
 * of its purity.
 */
@Mixin(value = FluidUtil.class, remap = false)
public abstract class MixinFluidUtil
{
    @ModifyReturnValue(method = "tryPlaceFluid(Lnet/neoforged/neoforge/transfer/fluid/FluidResource;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"))
    private static boolean droplets_of_thirst$registerPouredWater(boolean placed, FluidResource resource, @Nullable Player player, Level level, InteractionHand hand, BlockPos pos)
    {
        if (placed && !level.isClientSide() && resource.is(FluidTags.WATER))
            PouredWater.poured(level, pos, WaterPurity.sanitizePurity(resource.get(ThirstComponent.PURITY)));
        return placed;
    }
}
