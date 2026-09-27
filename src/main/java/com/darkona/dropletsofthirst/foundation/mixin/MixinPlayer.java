package com.darkona.dropletsofthirst.foundation.mixin;

import com.darkona.dropletsofthirst.content.thirst.PlayerThirstManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * OWN mode: an attack costs thirst where vanilla charges it hunger, only when the hit lands. A swing that another mod
 * cancels, or one at an invulnerable target, costs nothing. Hooked on vanilla's own charge because no event comes after
 * a landed hit for every target: {@code LivingDamageEvent} misses boats, minecarts and end crystals, comes once per mob
 * a sweep hits instead of once per swing, and a spear's stab posts no attack event at all.
 */
@Mixin(Player.class)
public abstract class MixinPlayer
{
    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void droplets_of_thirst$attackLanded(Entity target, CallbackInfo ci)
    {
        if ((Object) this instanceof ServerPlayer player)
            PlayerThirstManager.attackLanded(player);
    }

    @Inject(method = "stabAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void droplets_of_thirst$stabLanded(EquipmentSlot slot, Entity target, float baseDamage, boolean dealsDamage, boolean dealsKnockback, boolean dismounts, CallbackInfoReturnable<Boolean> cir)
    {
        if ((Object) this instanceof ServerPlayer player)
            PlayerThirstManager.attackLanded(player);
    }
}
