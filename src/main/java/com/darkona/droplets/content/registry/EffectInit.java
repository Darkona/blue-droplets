package com.darkona.droplets.content.registry;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.foundation.common.effect.DehydrationEffect;
import com.darkona.droplets.foundation.common.effect.QuenchnessEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EffectInit {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, BlueDroplets.ID);
    public static final DeferredHolder<MobEffect, MobEffect> QUENCHNESS = MOB_EFFECTS.register("quenchness", () -> new QuenchnessEffect(MobEffectCategory.BENEFICIAL, 0x3FB8E6));
    public static final DeferredHolder<MobEffect, MobEffect> DEHYDRATION = MOB_EFFECTS.register("dehydration", () -> new DehydrationEffect(MobEffectCategory.HARMFUL, 0xA0621F));
    public static final DeferredHolder<MobEffect, MobEffect> HYDRATED = MOB_EFFECTS.register("hydrated", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x7FE0C0) {});
    public static final DeferredHolder<MobEffect, MobEffect> OVERHYDRATED = MOB_EFFECTS.register("overhydrated", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x9DB0C0) {}
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, BlueDroplets.asResource("effect.overhydrated"), -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(BuiltInRegistries.POTION, BlueDroplets.ID);
    public static final DeferredHolder<Potion, Potion> QUENCHNESS_POTION = POTIONS.register("quenchness", () -> new Potion("quenchness", new MobEffectInstance(QUENCHNESS, 900)));
    public static final DeferredHolder<Potion, Potion> LONG_QUENCHNESS_POTION = POTIONS.register("long_quenchness", () -> new Potion("quenchness", new MobEffectInstance(QUENCHNESS, 1800)));
    public static final DeferredHolder<Potion, Potion> STRONG_QUENCHNESS_POTION = POTIONS.register("strong_quenchness", () -> new Potion("quenchness", new MobEffectInstance(QUENCHNESS, 450, 1)));

    // The brewing mixes are minecraft:brewing recipes (data/blue_droplets/recipe/brewing, scripts/brewing/generate.py)
    // that load only with gameplay.toml effects.quenchnessPotion (load condition blue_droplets:quenchness_potion).
    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
        POTIONS.register(eventBus);
    }
}
