package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.PouredWater;
import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * An empty bucket (vanilla, or another mod's that keeps {@code BucketItem.use}) picking up water gets the purity of
 * that water, on the server. Changed in place, so the advancement trigger sees it too. A water bucket emptied into
 * the world (by a player or a dispenser) registers the source it leaves as poured water of its purity.
 */
@Mixin(BucketItem.class)
public abstract class MixinBucketItem
{
    @ModifyArg(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", ordinal = 0), index = 2)
    private ItemStack blue_droplets$worldWaterPurity(ItemStack filled, @Local(argsOnly = true) Level level, @Local(ordinal = 0) BlockPos pos)
    {
        if (!level.isClientSide() && WaterPurity.enabled() && WaterPurity.isWaterFilledContainer(filled) && !WaterPurity.hasPurity(filled))
            WaterPurity.addPurity(filled, WaterPurity.takenWaterPurity(level, pos));
        return filled;
    }

    @ModifyReturnValue(method = "emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private boolean blue_droplets$registerPouredWater(boolean placed, @Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult result, @Nullable ItemStack container)
    {
        if (placed && !level.isClientSide())
            PouredWater.poured(level, pos, container != null && WaterPurity.isWaterFilledContainer(container) ? WaterPurity.getPurity(container) : WaterPurity.defaultPurity());
        return placed;
    }
}
