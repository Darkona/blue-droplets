package com.darkona.dropletsofthirst.foundation.mixin.vampirism;

import com.darkona.dropletsofthirst.compat.vampirism.VampirismCompat;
import de.teamlapen.vampirism.entity.player.vampire.VampirePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every drink of blood by a vampire player also restores thirst ({@link VampirismCompat#bloodDrunk}). Vampirism
 * 1.10 posts {@code PlayerDrinkBloodEvent} here; this version has no event, so this is the one hook.
 */
@Mixin(value = VampirePlayer.class, remap = false)
public abstract class MixinVampirePlayer
{
    @Inject(method = "drinkBlood(IFZ)V", at = @At("HEAD"))
    private void droplets_of_thirst$drinkBlood(int amount, float saturationMod, boolean useRemaining, CallbackInfo ci)
    {
        VampirismCompat.bloodDrunk(((VampirePlayer) (Object) this).getRepresentingPlayer(), amount, saturationMod);
    }
}
