package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.foundation.config.GameplayConfig;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class MixinFoodData
{
    @Shadow
    public abstract void addExhaustion(float p_38704_);

    @Shadow private float exhaustionLevel;
    @Unique
    private int dehydratedHealTimer = 0;


    @WrapOperation(
            method = {"tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V", ordinal = 0)
    )
    private void healWithSaturation(ServerPlayer player, float amount, Operation<Void> original)
    {
        FoodData foodData = player.getFoodData();
        PlayerThirst thirstData = player.getData(ModAttachment.PLAYER_THIRST);

        float f = Math.min(foodData.getSaturationLevel(), 6.0F);

        boolean shouldHeal = !GameplayConfig.REGEN_HALTED_WHEN_THIRSTY.get() || thirstData.getThirst() >= GameplayConfig.FULL_REGEN_MIN_THIRST.get();

        if(shouldHeal)
        {
            original.call(player, amount);
            thirstData.onFoodHeal(amount);
            return;
        }

        dehydratedHealTimer++;
        if(dehydratedHealTimer >= GameplayConfig.SLOW_REGEN_INTERVAL_TICKS.get() && thirstData.getThirst() >= GameplayConfig.SLOW_REGEN_MIN_THIRST.get())
        {
            original.call(player, amount);
            thirstData.onFoodHeal(amount);
            dehydratedHealTimer = 0;
            return;
        }

        this.addExhaustion(-f);
    }

    @WrapOperation(
            method = {"tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V", ordinal = 1)
    )
    private void healWithHunger(ServerPlayer player, float amount, Operation<Void> original)
    {
        PlayerThirst thirstData = player.getData(ModAttachment.PLAYER_THIRST);
        boolean shouldHeal = !GameplayConfig.REGEN_HALTED_WHEN_THIRSTY.get() || thirstData.getThirst() >= GameplayConfig.HUNGER_REGEN_MIN_THIRST.get();

        if(shouldHeal)
        {
            original.call(player, amount);
            thirstData.onFoodHeal(amount);
        }
        else
            this.addExhaustion(-6.0F);
    }

    @Inject(method = "tick",at = @At(value = "HEAD"))
    private void DealWithExhaustionBySaturation(ServerPlayer player, CallbackInfo ci){
        if(exhaustionLevel>4.0F){
           player.getData(ModAttachment.PLAYER_THIRST).ExhaustionRecalculate();
        }
    }
}
