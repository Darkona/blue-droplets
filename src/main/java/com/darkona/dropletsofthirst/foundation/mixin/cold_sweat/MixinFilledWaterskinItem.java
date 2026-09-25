package com.darkona.dropletsofthirst.foundation.mixin.cold_sweat;

import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.momosoftworks.coldsweat.common.item.FilledWaterskinItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * An emptied waterskin loses its purity, so it stacks with the others again. Cold Sweat builds every empty waterskin
 * (drinking the last sip, pouring, crafting) here by copying all the filled one's components; it only drops the purity
 * of Thirst Was Taken.
 */
@Mixin(value = FilledWaterskinItem.class, remap = false)
public abstract class MixinFilledWaterskinItem
{
    @ModifyReturnValue(method = "getContainerItem", at = @At("RETURN"))
    private ItemStack droplets_of_thirst$dropPurity(ItemStack empty)
    {
        if (!(empty.getItem() instanceof FilledWaterskinItem))
            ThirstComponent.remove(empty);
        return empty;
    }
}
