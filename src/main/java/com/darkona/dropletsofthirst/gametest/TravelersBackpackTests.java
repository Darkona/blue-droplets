package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.api.event.ThirstChangeEvent;
import com.darkona.dropletsofthirst.api.PurityLevel;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.tiviacz.travelersbackpack.api.fluids.EffectFluid;
import com.tiviacz.travelersbackpack.fluids.EffectFluidRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.darkona.dropletsofthirst.gametest.TestSupport.player;
import static com.darkona.dropletsofthirst.gametest.TestSupport.thirst;

import static com.darkona.dropletsofthirst.gametest.TestSupport.assertValueEqual;

/**
 * Traveler's Backpack: the hose water effect. Registered by {@link DropletsGameTests} only when it is installed.
 */
@PrefixGameTestTemplate(false)
public class TravelersBackpackTests
{
    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void hoseWaterHydratesWithTheTankPurity(GameTestHelper helper)
    {
        EffectFluid effect = EffectFluidRegistry.getRegisteredFluidEffects().get("droplets_of_thirst:water");
        helper.assertTrue(effect != null, "the hose water effect is not registered");
        helper.assertTrue(effect.fluid == Fluids.WATER, "the hose water effect is not for water");
        helper.assertTrue(EffectFluidRegistry.getEffectsForFluid(Fluids.WATER).size() == 1, "Traveler's Backpack's own water effect is still registered");
        assertValueEqual(helper, EffectFluidRegistry.getHighestFluidEffectAmount(Fluids.WATER), 250, "mB drained per sip");

        FluidStack sip = WaterPurity.addPurity(new FluidStack(Fluids.WATER, 1000), PurityLevel.PURE.level());
        ServerPlayer player = player(helper);
        helper.assertTrue(effect.canExecuteEffect(sip, helper.getLevel(), player), "cannot drink 1000 mB");
        helper.assertFalse(effect.canExecuteEffect(new FluidStack(sip, 100), helper.getLevel(), player), "can drink 100 mB");

        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        effect.affectDrinker(sip, helper.getLevel(), player);
        assertValueEqual(helper, thirst.getThirst(), 10, "thirst after a sip of pure water (a water bottle: 4, + 2 pure)");
        assertValueEqual(helper, thirst.getQuenched(), 8, "quenched after a sip of pure water (5 + 3 pure)");
        helper.succeed();
    }
}
