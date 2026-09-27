package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.purity.PouredWater;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * An empty bucket (vanilla, or another mod's that keeps {@code BucketItem.use}) picking up water gets the purity of
 * that water, on the server. Changed in place, so the advancement trigger sees it too. A water bucket emptied into
 * the world (by a player or a dispenser) registers the source it leaves as poured water of its purity.
 */
@Mixin(BucketItem.class)
public abstract class MixinBucketItem
{
    // The bucket's only pickupBlock call: unlike createFilledResult (the second call in use since Minecraft 26.2), it
    // cannot shift to another ordinal.
    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack droplets_of_thirst$worldWaterPurity(BucketPickup block, @Nullable LivingEntity user, LevelAccessor levelAccessor, BlockPos pos, BlockState state, Operation<ItemStack> original, @Local(argsOnly = true) Level level)
    {
        ItemStack filled = original.call(block, user, levelAccessor, pos, state);
        if (!level.isClientSide() && WaterPurity.enabled() && WaterPurity.isWaterFilledContainer(filled) && !WaterPurity.hasPurity(filled))
            WaterPurity.addPurity(filled, WaterPurity.takenWaterPurity(level, pos));
        return filled;
    }

    @ModifyReturnValue(method = "emptyContents(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"))
    private boolean droplets_of_thirst$registerPouredWater(boolean placed, @Nullable LivingEntity user, Level level, BlockPos pos, @Nullable BlockHitResult result, @Nullable ItemStack container)
    {
        if (placed && !level.isClientSide())
            PouredWater.poured(level, pos, container != null && WaterPurity.isWaterFilledContainer(container) ? WaterPurity.getPurity(container) : WaterPurity.defaultPurity());
        return placed;
    }
}
