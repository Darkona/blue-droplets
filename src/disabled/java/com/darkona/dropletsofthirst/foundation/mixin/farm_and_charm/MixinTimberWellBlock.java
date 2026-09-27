package com.darkona.dropletsofthirst.foundation.mixin.farm_and_charm;

import com.darkona.dropletsofthirst.compat.delight.DelightCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.satisfy.farm_and_charm.core.block.TimberWellBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Buckets filled at the Farm & Charm timber well carry groundwater: the purity of the world's water where it stands.
 */
@Mixin(value = TimberWellBlock.class, remap = false)
public abstract class MixinTimberWellBlock
{
    @ModifyArg(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"), index = 2)
    private ItemStack droplets_of_thirst$worldPurity(ItemStack filled, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos)
    {
        return DelightCompat.withSourcePurity(filled, level, pos);
    }
}
