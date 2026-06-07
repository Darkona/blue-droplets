package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.compat.coldsweat.ColdSweatCompat;
import com.darkona.droplets.compat.supernatural.SupernaturalCompat;
import com.darkona.droplets.compat.vampirism.VampirismCompat;
import com.darkona.droplets.content.thirst.ExhaustionFactors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.droplets.gametest.TestSupport.player;

/**
 * Optional mods, with or without them on the runtime ({@code -PwithCompat}): what Blue Droplets registers and calls
 * must follow what is installed, and every bridge must answer on a real player. Runs on a dedicated server, so it also
 * shows that none of this reaches client-only code.
 */
@GameTestHolder(BlueDroplets.ID)
@PrefixGameTestTemplate(false)
public class CompatTests
{
    @GameTest(template = "empty")
    public static void contentFollowsInstalledMods(GameTestHelper helper)
    {
        helper.assertTrue(FMLEnvironment.dist.isDedicatedServer(), "GameTests should run on a dedicated server");
        boolean create = ModList.get().isLoaded("create");
        helper.assertValueEqual(BuiltInRegistries.BLOCK.containsKey(BlueDroplets.asResource("sand_filter")), create, "Sand Filter registered with Create installed");
        helper.assertValueEqual(ColdSweatCompat.LOADED, ModList.get().isLoaded("coldsweat"), "Cold Sweat detected");
        helper.assertValueEqual(VampirismCompat.LOADED, ModList.get().isLoaded("vampirism"), "Vampirism detected");
        helper.assertValueEqual(SupernaturalCompat.LOADED, ModList.get().isLoaded("supernatural"), "Supernatural detected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyBridgeAnswers(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        helper.assertTrue(Double.isFinite(ColdSweatCompat.bodyTemperature(player)), "Cold Sweat body temperature");
        helper.assertFalse(VampirismCompat.isVampire(player), "a new player is a Vampirism vampire");
        helper.assertFalse(SupernaturalCompat.isVampire(player), "a new player is a Supernatural vampire");
        helper.assertTrue(SupernaturalCompat.canDrinkItem(new ItemStack(Items.POTION), player), "a new player cannot drink a potion");
        float[] factors = new float[ExhaustionFactors.FACTORS.length];
        float total = ExhaustionFactors.compute(player, factors);
        helper.assertTrue(Float.isFinite(total) && total >= 0, "thirst loss multiplier " + total);
        helper.succeed();
    }
}
