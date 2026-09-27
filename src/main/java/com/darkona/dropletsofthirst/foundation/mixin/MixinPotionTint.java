package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.purity.PurityTint;
import com.darkona.dropletsofthirst.foundation.config.ClientConfig;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.color.item.Potion;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Potion.class)
public class MixinPotionTint
{
    /**
     * Tints the liquid of water bottles by purity: the potion tint source of vanilla's potion item model. Only the
     * result changes, and only for water with a stored purity, so other potions keep their colors.
     */
    @ModifyReturnValue(method = "calculate", at = @At("RETURN"))
    private int droplets_of_thirst$tintWaterByPurity(int original, ItemStack stack)
    {
        return ClientConfig.TINT_WATER_BY_PURITY.get() ? PurityTint.color(stack, 0, original) : original;
    }
}
