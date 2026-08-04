package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.thirst.PlayerThirst;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Food hydrates where it feeds the player: the food component's part of consuming an item, with the stack before it
 * shrinks. Water containers hydrate in {@code LivingEntityUseItemEvent.Finish} instead.
 */
@Mixin(FoodProperties.class)
public abstract class MixinFoodProperties
{
    @Inject(method = "onConsume", at = @At("HEAD"))
    private void blue_droplets$hydrate(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo ci)
    {
        if (user instanceof Player player && !WaterPurity.isWaterFilledContainer(stack))
            PlayerThirst.consume(stack, player);
    }
}
