package com.darkona.dropletsofthirst.foundation.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * An effect with no behavior of its own, for effects that only carry attribute modifiers or that other code checks
 * for. A named class: the anonymous {@code new MobEffect(...) {}} it replaces did not compile on a clean CI setup
 * ("variable o is already defined in constructor"), while this compiles everywhere.
 */
public class SimpleEffect extends MobEffect {
    public SimpleEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
