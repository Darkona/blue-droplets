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
     * Prevents sprinting when thirsty, using the server's rule from the last sync: the player counts as having too
     * little food to sprint while thirst is too low. Riding and flying still allow it, as vanilla checks them first.
     * Minecraft 1.21.11 checks this every tick while sprinting too, so thirst stops a sprint as hunger does.
     */
    @ModifyExpressionValue(method = "isSprintingPossible", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;hasEnoughFoodToDoExhaustiveManoeuvres()Z"))
    private boolean hasEnoughThirstToSprint(boolean food){
        LocalPlayer player = (LocalPlayer) (Object) this;
        PlayerThirst thirst = player.getData(ModAttachment.PLAYER_THIRST);
        if(!food || !thirst.isSprintBlocked() || thirst.getThirst() > thirst.sprintMinThirst() || player.mayFly())
            return food;
        return false;
    }
}
