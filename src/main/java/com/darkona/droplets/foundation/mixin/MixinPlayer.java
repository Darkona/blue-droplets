package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.content.thirst.PlayerThirstManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayer
{
    @Inject(method = "eat", at = @At("HEAD"))
    public void onEatDrink(Level level, ItemStack food, CallbackInfoReturnable<ItemStack> cir)
    {
        Player player = (Player) (Object) this;
        if (!WaterPurity.isWaterFilledContainer(food))
            PlayerThirst.consume(food, player);
    }

    /**
     * OWN mode: an attack costs thirst where vanilla charges it hunger, only when the hit lands. A swing that another
     * mod cancels, or one at an invulnerable target, costs nothing. Hooked on vanilla's own charge because no event
     * comes after a landed hit for every target: {@code LivingDamageEvent} misses boats, minecarts and end crystals,
     * and comes once per mob a sweep hits instead of once per swing.
     */
    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void blue_droplets$attackLanded(Entity target, CallbackInfo ci)
    {
        if ((Object) this instanceof ServerPlayer player)
            PlayerThirstManager.attackLanded(player);
    }

}