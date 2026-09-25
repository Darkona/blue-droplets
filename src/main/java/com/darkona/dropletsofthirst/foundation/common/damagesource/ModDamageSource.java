package com.darkona.dropletsofthirst.foundation.common.damagesource;

import net.minecraft.world.damagesource.DamageSource;

public class ModDamageSource
{
    /**
     * Dehydration: like starving, it goes through armor, enchantments and Resistance, and costs no exhaustion. Minecraft
     * 1.18.2 has no damage types, so it is a plain damage source with the message id {@code dehydrate}.
     */
    public static final DamageSource DIE_OF_THIRST = new DamageSource("dehydrate").bypassArmor().bypassMagic();

}
