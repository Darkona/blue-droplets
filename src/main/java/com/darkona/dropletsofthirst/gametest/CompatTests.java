package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.compat.sereneseasons.SereneSeasonsCompat;
import com.darkona.dropletsofthirst.content.thirst.ExhaustionFactors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;

import static com.darkona.dropletsofthirst.gametest.TestSupport.player;

/**
 * Optional mods, with or without them on the runtime ({@code -PwithCompat}): what Droplets of Thirst registers and calls
 * must follow what is installed, and every bridge must answer on a real player. Runs on a dedicated server, so it also
 * shows that none of this reaches client-only code.
 */
public class CompatTests
{
    @GameTest(template = "empty")
    public static void contentFollowsInstalledMods(GameTestHelper helper)
    {
        helper.assertTrue(FMLEnvironment.getDist().isDedicatedServer(), "GameTests should run on a dedicated server");
        helper.assertValueEqual(SereneSeasonsCompat.LOADED, BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("sereneseasons", "calendar")), "Serene Seasons detected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyBridgeAnswers(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        float[] factors = new float[ExhaustionFactors.FACTORS.length];
        float total = ExhaustionFactors.compute(player, factors);
        helper.assertTrue(Float.isFinite(total) && total >= 0, "thirst loss multiplier " + total);
        helper.succeed();
    }
}
