package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class MixinFoodData
{
    @Shadow
    public abstract void addExhaustion(float p_38704_);

    @Shadow private float exhaustionLevel;
    @Unique
    private int dehydratedHealTimer = 0;


    @Redirect(
            method = {"tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V", ordinal = 0)
    )
    private void healWithSaturation(Player player, float amount)
    {
        FoodData foodData = player.getFoodData();
        PlayerThirst thirstData = player.getData(ModAttachment.PLAYER_THIRST);

        float f = Math.min(foodData.getSaturationLevel(), 6.0F);

        boolean shouldHeal = !GameplayConfig.REGEN_HALTED_WHEN_THIRSTY.get() || thirstData.getThirst() >= GameplayConfig.FULL_REGEN_MIN_THIRST.get();

        if(shouldHeal)
        {
            player.heal(f / 6.0F);
            thirstData.onFoodHeal(f / 6.0F);
            return;
        }

        dehydratedHealTimer++;
        if(dehydratedHealTimer >= GameplayConfig.SLOW_REGEN_INTERVAL_TICKS.get() && thirstData.getThirst() >= GameplayConfig.SLOW_REGEN_MIN_THIRST.get())
        {
            player.heal(f / 6.0F);
            thirstData.onFoodHeal(f / 6.0F);
            dehydratedHealTimer = 0;
            return;
        }

        this.addExhaustion(-f);
    }

    @Redirect(
            method = {"tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V", ordinal = 1)
    )
    private void healWithHunger(Player player, float amount)
    {
        PlayerThirst thirstData = player.getData(ModAttachment.PLAYER_THIRST);
        boolean shouldHeal = !GameplayConfig.REGEN_HALTED_WHEN_THIRSTY.get() || thirstData.getThirst() >= GameplayConfig.HUNGER_REGEN_MIN_THIRST.get();

        if(shouldHeal)
        {
            player.heal(1.0F);
            thirstData.onFoodHeal(1.0F);
        }
        else
            this.addExhaustion(-6.0F);
    }

    @Inject(method = "tick",at = @At(value = "HEAD"))
    private void DealWithExhaustionBySaturation(Player player, CallbackInfo ci){
        if(exhaustionLevel>4.0F){
           player.getData(ModAttachment.PLAYER_THIRST).ExhaustionRecalculate();
        }
    }
}
