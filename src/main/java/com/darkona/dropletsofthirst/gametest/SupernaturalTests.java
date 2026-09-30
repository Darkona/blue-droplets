package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.api.event.ThirstChangeEvent;
import com.darkona.dropletsofthirst.compat.supernatural.SupernaturalCompat;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.salju.supernatural.events.SupernaturalManager;

import static com.darkona.dropletsofthirst.gametest.TestSupport.player;
import static com.darkona.dropletsofthirst.gametest.TestSupport.thirst;

/**
 * Supernatural: a vampire loses thirst like anyone, but only blood hydrates it. Registered by {@link DropletsGameTests}
 * only when Supernatural is installed.
 */
public class SupernaturalTests
{
    @GameTest(template = "empty")
    public static void onlyBloodHydratesAVampire(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        ItemStack blood = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("supernatural", "blood_bottle")));
        helper.assertFalse(blood.isEmpty(), "supernatural:blood_bottle does not exist");
        helper.assertTrue(ThirstHelper.playerRestoresThirst(water, player), "water does not restore a new player's thirst");
        try
        {
            SupernaturalManager.setVampire(player, true);
            helper.assertTrue(SupernaturalCompat.isVampire(player), "the test player did not become a vampire");
            helper.assertFalse(ThirstHelper.playerRestoresThirst(water, player), "water restores a vampire's thirst");
            thirst(player).change(player, 10, 0, ThirstChangeEvent.Cause.COMMAND);
            PlayerThirst.consume(water, player);
            helper.assertValueEqual(thirst(player).getThirst(), 10, "a vampire's thirst after a water bottle");
            PlayerThirst.consume(blood, player);
            helper.assertValueEqual(thirst(player).getThirst(), 16, "a vampire's thirst after a blood bottle");
            helper.assertValueEqual(thirst(player).getQuenched(), 6, "a vampire's quenched after a blood bottle");
        }
        finally
        {
            SupernaturalManager.setVampire(player, false);
        }
        helper.succeed();
    }
}
