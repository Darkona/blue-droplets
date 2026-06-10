package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * An empty bucket (vanilla, or another mod's that keeps {@code BucketItem.use}) picking up water gets the purity of
 * that water, on the server. Changed in place, so the advancement trigger sees it too.
 */
@Mixin(BucketItem.class)
public abstract class MixinBucketItem
{
    @ModifyArg(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", ordinal = 0), index = 2)
    private ItemStack bluedroplets$worldWaterPurity(ItemStack filled, @Local(argsOnly = true) Level level, @Local(ordinal = 0) BlockPos pos)
    {
        if (!level.isClientSide() && WaterPurity.isWaterFilledContainer(filled) && !WaterPurity.hasPurity(filled))
            WaterPurity.addPurity(filled, WaterPurity.takenWaterPurity(level, pos));
        return filled;
    }
}
