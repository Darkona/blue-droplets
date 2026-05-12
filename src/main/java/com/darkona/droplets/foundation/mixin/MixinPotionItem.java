package com.darkona.droplets.foundation.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stackable water bottles: vanilla only adds the empty bottle to the inventory and loses it when full; drop it instead.
 * Purity effects and hydration are in {@code PlayerThirstManager#drink} (LivingEntityUseItemEvent.Finish).
 */
@Mixin(PotionItem.class)
public class MixinPotionItem {

    @Redirect(method = "finishUsingItem",at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    public boolean finishUsingItem(Inventory instance, ItemStack stack){
        if (!instance.add(stack)) {
            instance.player.drop(stack, false);
        }
        return true;
    }
}
