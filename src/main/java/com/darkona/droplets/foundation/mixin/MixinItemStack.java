package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.registry.ThirstComponent;
import com.darkona.droplets.foundation.config.SyncedValues;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack
{
    @Shadow public abstract Item getItem();

    @Inject(method="getMaxStackSize", at = @At("HEAD"), cancellable = true)
    public void changeWaterBottleStackSize(CallbackInfoReturnable<Integer> cir)
    {
        if(getItem() != Items.POTION)
            return;
        if(PotionUtils.getPotion((ItemStack) (Object) this) == Potions.WATER)
            cir.setReturnValue(SyncedValues.waterBottleStackSize());
    }

    /**
     * Stacks read from a save: a Thirst Was Taken purity becomes a purity on the new scale.
     */
    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void blue_droplets$migrateLegacyPurity(CompoundTag tag, CallbackInfo ci)
    {
        ThirstComponent.migrateLegacyPurity((ItemStack) (Object) this);
    }
}
