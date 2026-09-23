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
     * thirst is too low to sprint. Flying still allows it, as vanilla checks it too. In 1.19.2 the food check is
     * inline in {@code aiStep}, the only place that reads the food level.
     */
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;getFoodLevel()I"))
    private int hasEnoughThirstToStartSprinting(int food){
        PlayerThirst thirst = ModAttachment.thirst((LocalPlayer) (Object) this);
        if(!thirst.isSprintBlocked() || thirst.getThirst() > thirst.sprintMinThirst())
            return food;
        return 0;
    }
}
