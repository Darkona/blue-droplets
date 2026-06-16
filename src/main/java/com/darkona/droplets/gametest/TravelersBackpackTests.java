package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.event.ThirstChangeEvent;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.tiviacz.travelersbackpack.api.fluids.EffectFluid;
import com.tiviacz.travelersbackpack.fluids.EffectFluidRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.droplets.gametest.TestSupport.player;
import static com.darkona.droplets.gametest.TestSupport.thirst;

/**
 * Traveler's Backpack: the hose water effect. Registered by {@link DropletsGameTests} only when it is installed.
 */
@PrefixGameTestTemplate(false)
public class TravelersBackpackTests
{
    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void hoseWaterHydratesWithTheTankPurity(GameTestHelper helper)
    {
        EffectFluid effect = EffectFluidRegistry.getRegisteredFluidEffects().get("bluedroplets:water");
        helper.assertTrue(effect != null, "the hose water effect is not registered");
        helper.assertTrue(effect.fluid == Fluids.WATER, "the hose water effect is not for water");
        helper.assertTrue(EffectFluidRegistry.getEffectsForFluid(Fluids.WATER).size() == 1, "Traveler's Backpack's own water effect is still registered");
        helper.assertValueEqual(EffectFluidRegistry.getHighestFluidEffectAmount(Fluids.WATER), 250, "mB drained per sip");

        FluidStack sip = WaterPurity.addPurity(new FluidStack(Fluids.WATER, 1000), WaterPurity.MAX_PURITY);
        ServerPlayer player = player(helper);
        helper.assertTrue(effect.canExecuteEffect(sip, helper.getLevel(), player), "cannot drink 1000 mB");
        helper.assertFalse(effect.canExecuteEffect(sip.copyWithAmount(100), helper.getLevel(), player), "can drink 100 mB");

        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        effect.affectDrinker(sip, helper.getLevel(), player);
        helper.assertValueEqual(thirst.getThirst(), 10, "thirst after a sip of purified water (a water bottle: 6)");
        helper.assertValueEqual(thirst.getQuenched(), 8, "quenched after a sip of purified water (8)");
        helper.succeed();
    }
}
