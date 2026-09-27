package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import com.darkona.dropletsofthirst.foundation.dev.ItemDump;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;
import java.util.Set;

/**
 * Development tools: the item dump behind {@code /droplets_of_thirst dev dump_items}, which only exists outside production.
 */
public class DevTests
{
    @GameTest(template = "empty")
    public static void itemDumpListsFoodDrinksAndCurrentValues(GameTestHelper helper)
    {
        helper.assertValueEqual(helper.getLevel().getServer().getCommands().getDispatcher().getRoot()
                .getChild(DropletsOfThirst.ID).getChild("dev") != null, !FMLEnvironment.isProduction(), "dev commands registered outside production");

        List<String> rows = ItemDump.rows(Set.of("minecraft"));
        helper.assertValueEqual(rows.get(0), ItemDump.HEADER, "header");
        helper.assertTrue(rows.stream().allMatch(row -> row.startsWith("minecraft:") || row == rows.get(0)), "only the asked namespace");
        String potion = row(rows, "minecraft:potion");
        helper.assertTrue(potion.startsWith("minecraft:potion,minecraft,true,,,,," + ThirstHelper.getThirst(Items.POTION.getDefaultInstance())
                + "," + ThirstHelper.getQuenched(Items.POTION.getDefaultInstance()) + ","), "water bottle row: " + potion);
        helper.assertTrue(row(rows, "minecraft:golden_apple").startsWith("minecraft:golden_apple,minecraft,false,4,9.60,\"minecraft:regeneration,100,1,1.00;minecraft:absorption,2400,0,1.00\",true,"),
                "golden apple row: " + row(rows, "minecraft:golden_apple"));
        helper.assertTrue(row(rows, "minecraft:oak_log").contains("minecraft:logs"), "tags of the oak log");

        List<String> all = ItemDump.rows(Set.of(ItemDump.ALL));
        helper.assertFalse(row(all, "minecraft:apple").isEmpty(), "* leaves out minecraft");
        helper.assertFalse(row(all, "droplets_of_thirst:terracotta_water_bowl").isEmpty(), "* leaves out droplets_of_thirst");
        helper.succeed();
    }

    private static String row(List<String> rows, String id)
    {
        return rows.stream().filter(row -> row.startsWith(id + ",")).findFirst().orElse("");
    }
}
