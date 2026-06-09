package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cauldron interactions hand out the filled item through {@code createFilledResult} (the 3-argument overload calls this
 * one): during one, a water container that comes out gets the cauldron purity. Empty items poured back are not water
 * containers and are left alone.
 */
@Mixin(ItemUtils.class)
public abstract class MixinItemUtils
{
    @Inject(method = "createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private static void bluedroplets$cauldronPurity(ItemStack emptyStack, Player player, ItemStack filledStack, boolean preventDuplicates, CallbackInfoReturnable<ItemStack> cir)
    {
        WaterPurity.applyCauldronPurity(filledStack);
    }
}
