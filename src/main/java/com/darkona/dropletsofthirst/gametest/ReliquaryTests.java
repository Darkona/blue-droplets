package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import com.darkona.dropletsofthirst.api.event.ThirstChangeEvent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import reliquary.init.ModItems;

import static com.darkona.dropletsofthirst.gametest.TestSupport.player;
import static com.darkona.dropletsofthirst.gametest.TestSupport.thirst;

import static com.darkona.dropletsofthirst.gametest.TestSupport.assertValueEqual;

/**
 * Reliquary: the Emperor's Chalice hydrates, the Infernal Chalice does not, and the optional cooldown. Registered by
 * {@link DropletsGameTests} only when Reliquary is installed.
 */
@PrefixGameTestTemplate(false)
public class ReliquaryTests
{
    /** The full use path: the item's own finish, then the Finish event that droplets_of_thirst and the compat listen to. */
    private static void drink(GameTestHelper helper, ServerPlayer player, ItemStack chalice)
    {
        MinecraftForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Finish(player, chalice, 0, chalice.finishUsingItem(helper.getLevel(), player)));
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void emperorChaliceHydratesAndInfernalDoesNot(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        thirst(player).change(player, 10, 0, ThirstChangeEvent.Cause.COMMAND);
        int before = thirst(player).getThirst();
        int quenched = thirst(player).getQuenched();
        drink(helper, player, new ItemStack(ModItems.INFERNAL_CHALICE.get()));
        assertValueEqual(helper, thirst(player).getThirst(), before, "thirst after the Infernal Chalice");
        drink(helper, player, new ItemStack(ModItems.EMPEROR_CHALICE.get()));
        assertValueEqual(helper, thirst(player).getThirst(), before + 4, "thirst after the Emperor's Chalice");
        assertValueEqual(helper, thirst(player).getQuenched(), quenched + 5, "quenched after the Emperor's Chalice");
        assertValueEqual(helper, ThirstHelper.getDrinkPurity(new ItemStack(ModItems.EMPEROR_CHALICE.get())), 5, "purity of the Emperor's Chalice");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void emperorChaliceCooldownIsOptional(GameTestHelper helper)
    {
        int cooldown = CompatConfig.RELIQUARY_EMPEROR_CHALICE_COOLDOWN.get();
        try
        {
            ServerPlayer player = player(helper);
            ItemStack chalice = new ItemStack(ModItems.EMPEROR_CHALICE.get());
            assertValueEqual(helper, cooldown, 0, "cooldown default");
            drink(helper, player, chalice);
            helper.assertFalse(player.getCooldowns().isOnCooldown(chalice.getItem()), "the chalice is on cooldown with the cooldown off");
            TestSupport.set(CompatConfig.RELIQUARY_EMPEROR_CHALICE_COOLDOWN, 100);
            drink(helper, player, chalice);
            helper.assertTrue(player.getCooldowns().isOnCooldown(chalice.getItem()), "the chalice is not on cooldown after a drink");
            helper.assertFalse(player.getCooldowns().isOnCooldown(ModItems.INFERNAL_CHALICE.get()), "the Infernal Chalice shares the cooldown");
        }
        finally
        {
            TestSupport.set(CompatConfig.RELIQUARY_EMPEROR_CHALICE_COOLDOWN, cooldown);
        }
        helper.succeed();
    }
}
