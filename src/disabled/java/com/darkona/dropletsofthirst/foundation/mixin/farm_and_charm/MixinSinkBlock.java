package com.darkona.dropletsofthirst.foundation.mixin.farm_and_charm;

import com.darkona.dropletsofthirst.compat.delight.DelightCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.satisfy.farm_and_charm.core.block.SinkBlock;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The Farm & Charm sink fills itself from nowhere and hands out new water buckets and bottles: they get the purity
 * of the world's water where it stands. Only water containers change; the empty bucket or bottle it gives back when
 * filled is left alone.
 */
@Mixin(value = SinkBlock.class, remap = false)
public abstract class MixinSinkBlock
{
    @ModifyArg(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private ItemStack droplets_of_thirst$worldPurity(ItemStack given, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos)
    {
        return DelightCompat.withSourcePurity(given, level, pos);
    }
}
