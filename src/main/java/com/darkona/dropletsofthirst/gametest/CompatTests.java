package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.compat.sereneseasons.SereneSeasonsCompat;
import com.darkona.dropletsofthirst.compat.supernatural.SupernaturalCompat;
import com.darkona.dropletsofthirst.content.thirst.ExhaustionFactors;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
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
        helper.assertValueEqual(SupernaturalCompat.LOADED, ModList.get().isLoaded("supernatural"), "Supernatural detected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyBridgeAnswers(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        helper.assertFalse(SupernaturalCompat.isVampire(player), "a new player is a Supernatural vampire");
        helper.assertTrue(ThirstHelper.playerRestoresThirst(new ItemStack(Items.POTION), player), "a new player cannot drink a potion");
        float[] factors = new float[ExhaustionFactors.FACTORS.length];
        float total = ExhaustionFactors.compute(player, factors);
        helper.assertTrue(Float.isFinite(total) && total >= 0, "thirst loss multiplier " + total);
        helper.succeed();
    }
}
