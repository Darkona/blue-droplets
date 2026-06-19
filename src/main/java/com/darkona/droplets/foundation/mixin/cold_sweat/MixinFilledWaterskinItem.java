package com.darkona.droplets.foundation.mixin.cold_sweat;

import com.darkona.droplets.content.registry.ThirstComponent;
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
    @ModifyReturnValue(method = "getCraftingRemainingItem", at = @At("RETURN"))
    private ItemStack blue_droplets$dropPurity(ItemStack empty)
    {
        if (!(empty.getItem() instanceof FilledWaterskinItem))
            empty.remove(ThirstComponent.PURITY);
        return empty;
    }
}
