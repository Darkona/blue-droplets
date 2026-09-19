package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import com.darkona.dropletsofthirst.foundation.config.SyncedValues;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
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

    @Shadow public abstract DataComponentMap getComponents();

    @Inject(method="getMaxStackSize", at = @At("HEAD"), cancellable = true)
    public void changeWaterBottleStackSize(CallbackInfoReturnable<Integer> cir)
    {
        if(getItem() != Items.POTION)
            return;
        PotionContents contents = getComponents().get(DataComponents.POTION_CONTENTS);
        if(contents != null && contents.is(Potions.WATER))
            cir.setReturnValue(SyncedValues.waterBottleStackSize());
    }

    /**
     * Stacks read from a save or the network: a Thirst Was Taken purity becomes a purity on the new scale.
     */
    @Inject(method = "<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentPatch;)V", at = @At("TAIL"))
    private void droplets_of_thirst$migrateLegacyPurity(Holder<Item> item, int count, DataComponentPatch components, CallbackInfo ci)
    {
        if (!components.isEmpty())
            ThirstComponent.migrateLegacyPurity((ItemStack) (Object) this);
    }
}
