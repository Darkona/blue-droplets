package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.PurityTint;
import com.darkona.droplets.foundation.config.ClientConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemColors.class)
public class MixinItemColors{

    /**
     * Tints the liquid layer of water containers by purity. Only the result changes, and only for water with a stored
     * purity, so other potions and the color handlers other mods registered for them stay as they are.
     */
    @ModifyReturnValue(method = "getColor", at = @At("RETURN"))
    private int tintWaterByPurity(int original, ItemStack stack, int tintIndex){
        return tintIndex == 0 && ClientConfig.TINT_WATER_BY_PURITY.get() ? PurityTint.color(stack, tintIndex, original) : original;
    }
}
