package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.foundation.config.SyncedValues;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * A glass bottle filled from water in the world gets the purity of that water, on the server; with
 * {@code drinking.canFillFromFlowingWater} it also fills from flowing water. Dragon's breath is left alone.
 */
@Mixin(BottleItem.class)
public abstract class MixinBottleItem
{
    @ModifyArg(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BottleItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"), index = 2)
    private ClipContext.Fluid droplets_of_thirst$flowingWater(ClipContext.Fluid fluid)
    {
        return SyncedValues.canFillFromFlowingWater() ? ClipContext.Fluid.ANY : fluid;
    }

    @ModifyArg(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BottleItem;turnBottleIntoItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", ordinal = 1), index = 2)
    private ItemStack droplets_of_thirst$worldWaterPurity(ItemStack filled, @Local(argsOnly = true) Level level, @Local BlockPos pos)
    {
        if (!level.isClientSide() && WaterPurity.enabled() && WaterPurity.isWaterFilledContainer(filled) && !WaterPurity.hasPurity(filled))
            WaterPurity.addPurity(filled, WaterPurity.takenWaterPurity(level, pos));
        return filled;
    }
}
