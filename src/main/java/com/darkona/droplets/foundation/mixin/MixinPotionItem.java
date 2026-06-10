package com.darkona.droplets.foundation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stackable water bottles: vanilla only adds the empty bottle to the inventory and loses it when full; drop it instead.
 * Purity effects and hydration are in {@code PlayerThirstManager#drink} (LivingEntityUseItemEvent.Finish).
 */
@Mixin(PotionItem.class)
public class MixinPotionItem {

    @WrapOperation(method = "finishUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean finishUsingItem(Inventory inventory, ItemStack stack, Operation<Boolean> original){
        if (!original.call(inventory, stack)) {
            inventory.player.drop(stack, false);
        }
        return true;
    }
}
