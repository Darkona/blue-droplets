package com.darkona.dropletsofthirst.content.registry;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.common.effect.DehydrationEffect;
import com.darkona.dropletsofthirst.foundation.common.effect.QuenchnessEffect;
import com.darkona.dropletsofthirst.foundation.common.effect.SimpleEffect;
import com.darkona.dropletsofthirst.foundation.config.GameplayConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EffectInit {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DropletsOfThirst.ID);
    public static final RegistryObject<MobEffect> QUENCHNESS = MOB_EFFECTS.register("quenchness", () -> new QuenchnessEffect(MobEffectCategory.BENEFICIAL, 0x3FB8E6));
    public static final RegistryObject<MobEffect> DEHYDRATION = MOB_EFFECTS.register("dehydration", () -> new DehydrationEffect(MobEffectCategory.HARMFUL, 0xA0621F));
    public static final RegistryObject<MobEffect> HYDRATED = MOB_EFFECTS.register("hydrated", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x7FE0C0));
    public static final RegistryObject<MobEffect> OVERHYDRATED = MOB_EFFECTS.register("overhydrated", () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x9DB0C0)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "5E0C8F2A-6B1D-4C3E-9A7F-2D4B8C1E6F30", -0.1, AttributeModifier.Operation.MULTIPLY_TOTAL));

    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, DropletsOfThirst.ID);
    public static final RegistryObject<Potion> QUENCHNESS_POTION = POTIONS.register("quenchness", () -> new Potion("quenchness", new MobEffectInstance(QUENCHNESS.get(), 900)));
    public static final RegistryObject<Potion> LONG_QUENCHNESS_POTION = POTIONS.register("long_quenchness", () -> new Potion("quenchness", new MobEffectInstance(QUENCHNESS.get(), 1800)));
    public static final RegistryObject<Potion> STRONG_QUENCHNESS_POTION = POTIONS.register("strong_quenchness", () -> new Potion("quenchness", new MobEffectInstance(QUENCHNESS.get(), 450, 1)));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
        POTIONS.register(eventBus);
    }

    /**
     * Forge 1.18.2 has no brewing event: one recipe for the three mixes, which reads {@code effects.quenchnessPotion}
     * each time a brewing stand checks it, so turning it off needs no restart. Called once from common setup.
     */
    public static void registerBrewing() {
        BrewingRecipeRegistry.addRecipe(new QuenchnessBrewing());
    }

    /**
     * Awkward + prismarine crystals: Quenchness; Quenchness + redstone: long; + glowstone: strong. Potions, splash and
     * lingering potions keep their kind, like vanilla mixes.
     */
    private static final class QuenchnessBrewing implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            Item item = input.getItem();
            if (item != Items.POTION && item != Items.SPLASH_POTION && item != Items.LINGERING_POTION)
                return false;
            Potion potion = PotionUtils.getPotion(input);
            return potion == Potions.AWKWARD || potion == QUENCHNESS_POTION.get();
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.PRISMARINE_CRYSTALS) || ingredient.is(Items.REDSTONE) || ingredient.is(Items.GLOWSTONE_DUST);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!GameplayConfig.QUENCHNESS_POTION.get() || !isInput(input))
                return ItemStack.EMPTY;
            Potion potion = PotionUtils.getPotion(input);
            Potion result = null;
            if (potion == Potions.AWKWARD && ingredient.is(Items.PRISMARINE_CRYSTALS))
                result = QUENCHNESS_POTION.get();
            else if (potion == QUENCHNESS_POTION.get() && ingredient.is(Items.REDSTONE))
                result = LONG_QUENCHNESS_POTION.get();
            else if (potion == QUENCHNESS_POTION.get() && ingredient.is(Items.GLOWSTONE_DUST))
                result = STRONG_QUENCHNESS_POTION.get();
            return result == null ? ItemStack.EMPTY : PotionUtils.setPotion(new ItemStack(input.getItem()), result);
        }
    }
}
