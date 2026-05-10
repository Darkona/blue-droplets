package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.core.ThirstConstants;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer{

    /**
     * @reason prevent sprinting when thirsty, using the server's rule from the last sync
     * @return the food level, or 0 when thirst is too low to sprint
     */
    @Redirect(method ="hasEnoughFoodToStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;getFoodLevel()I"))
    public int hasEnoughThirstToStartSprinting(FoodData instance){
        int food = instance.getFoodLevel();
        PlayerThirst thirst = ((LocalPlayer) (Object) this).getData(ModAttachment.PLAYER_THIRST);
        if(!thirst.isSprintBlocked() || thirst.getThirst() > ThirstConstants.SPRINT_MIN_THIRST)
            return food;
        return 0;
    }
}
