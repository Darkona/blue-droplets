package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import com.darkona.dropletsofthirst.foundation.config.SyncedValues;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        if(WaterPurity.isWaterPotion((ItemStack) (Object) this))
            cir.setReturnValue(SyncedValues.waterBottleStackSize());
    }

    /**
     * Stacks read from a save: a Thirst Was Taken purity becomes a purity on the new scale.
     */
    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void droplets_of_thirst$migrateLegacyPurity(CompoundTag tag, CallbackInfo ci)
    {
        ThirstComponent.migrateLegacyPurity((ItemStack) (Object) this);
    }
}
