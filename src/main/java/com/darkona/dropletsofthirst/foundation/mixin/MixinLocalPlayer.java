package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer{

    /**
     * Prevents sprinting when thirsty, using the server's rule from the last sync: the food level reads as 0 when
     * thirst is too low to sprint. Riding and flying still allow it, as vanilla checks them first.
     */
    @ModifyExpressionValue(method = "hasEnoughFoodToStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;getFoodLevel()I"))
    private int hasEnoughThirstToStartSprinting(int food){
        PlayerThirst thirst = ((LocalPlayer) (Object) this).getData(ModAttachment.PLAYER_THIRST);
        if(!thirst.isSprintBlocked() || thirst.getThirst() > thirst.sprintMinThirst())
            return food;
        return 0;
    }
}
