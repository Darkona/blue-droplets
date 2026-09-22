package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.compat.coldsweat.ColdSweatCompat;
import com.darkona.dropletsofthirst.compat.sereneseasons.SereneSeasonsCompat;
import com.darkona.dropletsofthirst.compat.supernatural.SupernaturalCompat;
import com.darkona.dropletsofthirst.compat.vampirism.VampirismCompat;
import com.darkona.dropletsofthirst.content.thirst.ExhaustionFactors;
import com.darkona.dropletsofthirst.content.data.DropletsTags;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import com.darkona.dropletsofthirst.foundation.config.GameplayConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.darkona.dropletsofthirst.gametest.TestSupport.player;
import static com.darkona.dropletsofthirst.gametest.TestSupport.thirst;

import static com.darkona.dropletsofthirst.gametest.TestSupport.assertValueEqual;

/**
 * Optional mods, with or without them on the runtime ({@code -PwithCompat}): what Droplets of Thirst registers and calls
 * must follow what is installed, and every bridge must answer on a real player. Runs on a dedicated server, so it also
 * shows that none of this reaches client-only code.
 */
@GameTestHolder(DropletsOfThirst.ID)
@PrefixGameTestTemplate(false)
public class CompatTests
{
    @GameTest(template = "empty")
    public static void contentFollowsInstalledMods(GameTestHelper helper)
    {
        helper.assertTrue(FMLEnvironment.dist.isDedicatedServer(), "GameTests should run on a dedicated server");
        boolean create = ModList.get().isLoaded("create");
        assertValueEqual(helper, BuiltInRegistries.BLOCK.containsKey(DropletsOfThirst.asResource("sand_filter")), create, "Sand Filter registered with Create installed");
        assertValueEqual(helper, ColdSweatCompat.LOADED, BuiltInRegistries.ITEM.containsKey(new ResourceLocation("cold_sweat", "waterskin")), "Cold Sweat detected");
        assertValueEqual(helper, SereneSeasonsCompat.LOADED, BuiltInRegistries.ITEM.containsKey(new ResourceLocation("sereneseasons", "calendar")), "Serene Seasons detected");
        assertValueEqual(helper, VampirismCompat.LOADED, ModList.get().isLoaded("vampirism"), "Vampirism detected");
        assertValueEqual(helper, SupernaturalCompat.LOADED, ModList.get().isLoaded("supernatural"), "Supernatural detected");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyBridgeAnswers(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        helper.assertTrue(Double.isFinite(ColdSweatCompat.bodyTemperature(player)), "Cold Sweat body temperature");
        helper.assertFalse(VampirismCompat.isVampire(player), "a new player is a Vampirism vampire");
        helper.assertFalse(SupernaturalCompat.isVampire(player), "a new player is a Supernatural vampire");
        helper.assertTrue(ThirstHelper.playerRestoresThirst(new ItemStack(Items.POTION), player), "a new player cannot drink a potion");
        float[] factors = new float[ExhaustionFactors.FACTORS.length];
        float total = ExhaustionFactors.compute(player, factors);
        helper.assertTrue(Float.isFinite(total) && total >= 0, "thirst loss multiplier " + total);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void effectTagPausesThirst(GameTestHelper helper)
    {
        MobEffect nourishment = BuiltInRegistries.MOB_EFFECT.getOptional(new ResourceLocation("farmersdelight", "nourishment")).orElse(null);
        assertValueEqual(helper, nourishment != null && BuiltInRegistries.MOB_EFFECT.wrapAsHolder(nourishment).is(DropletsTags.PAUSES_THIRST), ModList.get().isLoaded("farmersdelight"), "Nourishment in droplets_of_thirst:pauses_thirst");
        if (nourishment == null)
        {
            helper.succeed();
            return;
        }
        double basal = GameplayConfig.BASAL_PER_TICK.get();
        try
        {
            TestSupport.set(GameplayConfig.BASAL_PER_TICK, 0.01);
            ServerPlayer player = player(helper);
            PlayerThirst thirst = thirst(player);
            float before = thirst.getExhaustion();
            thirst.tick(player);
            helper.assertTrue(thirst.getExhaustion() > before, "no thirst exhaustion without Nourishment");
            player.addEffect(new MobEffectInstance(nourishment, 200));
            before = thirst.getExhaustion();
            thirst.tick(player);
            assertValueEqual(helper, thirst.getExhaustion(), before, "thirst exhaustion with Nourishment");
        }
        finally
        {
            TestSupport.set(GameplayConfig.BASAL_PER_TICK, basal);
        }
        helper.succeed();
    }
}
