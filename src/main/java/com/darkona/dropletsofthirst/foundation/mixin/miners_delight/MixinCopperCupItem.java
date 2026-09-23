package com.darkona.dropletsofthirst.foundation.mixin.miners_delight;

import com.darkona.dropletsofthirst.content.purity.PouredWater;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.sammy.minersdelight.content.item.CopperCupItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Miner's Delight's copper cup is not a {@code BucketItem}: {@code CopperCupItem.use} picks up water itself and pours
 * it with its own {@code emptyContents}, so it needs the same pair of hooks as buckets ({@code MixinBucketItem}). The
 * cauldron is covered by the tag {@code purity_containers}. In 1.19.2 its {@code emptyContents} does not get the cup,
 * so pouring is caught where {@code use} calls it.
 */
@Mixin(CopperCupItem.class)
public abstract class MixinCopperCupItem
{
    @ModifyArg(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", ordinal = 0), index = 2)
    private ItemStack droplets_of_thirst$worldWaterPurity(ItemStack filled, @Local(argsOnly = true) Level level, @Local(ordinal = 0) BlockPos pos)
    {
        if (!level.isClientSide() && WaterPurity.enabled() && WaterPurity.isWaterFilledContainer(filled) && !WaterPurity.hasPurity(filled))
            WaterPurity.addPurity(filled, WaterPurity.takenWaterPurity(level, pos));
        return filled;
    }

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lcom/sammy/minersdelight/content/item/CopperCupItem;emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)Z"))
    private boolean droplets_of_thirst$registerPouredWater(CopperCupItem cup, Player player, Level level, BlockPos pos, BlockHitResult result, Operation<Boolean> original,
                                                      @Local(ordinal = 0) ItemStack container)
    {
        boolean placed = original.call(cup, player, level, pos, result);
        if (placed && !level.isClientSide())
            PouredWater.poured(level, pos, WaterPurity.isWaterFilledContainer(container) ? WaterPurity.getPurity(container) : WaterPurity.defaultPurity());
        return placed;
    }
}
