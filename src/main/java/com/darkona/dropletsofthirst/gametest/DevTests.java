package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import com.darkona.dropletsofthirst.foundation.dev.ItemDump;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

import static com.darkona.dropletsofthirst.gametest.TestSupport.assertValueEqual;
import static com.darkona.dropletsofthirst.gametest.TestSupport.assertTrue;
import static com.darkona.dropletsofthirst.gametest.TestSupport.assertFalse;

/**
 * Development tools: the item dump behind {@code /droplets_of_thirst dev dump_items}, which only exists outside production.
 */
@GameTestHolder(DropletsOfThirst.ID)
@PrefixGameTestTemplate(false)
public class DevTests
{
    @GameTest(template = "empty")
    public static void itemDumpListsFoodDrinksAndCurrentValues(GameTestHelper helper)
    {
        assertValueEqual(helper, helper.getLevel().getServer().getCommands().getDispatcher().getRoot()
                .getChild(DropletsOfThirst.ID).getChild("dev") != null, !FMLEnvironment.production, "dev commands registered outside production");

        List<String> rows = ItemDump.rows(Set.of("minecraft"));
        assertValueEqual(helper, rows.get(0), ItemDump.HEADER, "header");
        assertTrue(helper, rows.stream().allMatch(row -> row.startsWith("minecraft:") || row == rows.get(0)), "only the asked namespace");
        String potion = row(rows, "minecraft:potion");
        assertTrue(helper, potion.startsWith("minecraft:potion,minecraft,true,,,,," + ThirstHelper.getThirst(Items.POTION.getDefaultInstance())
                + "," + ThirstHelper.getQuenched(Items.POTION.getDefaultInstance()) + ","), "water bottle row: " + potion);
        assertTrue(helper, row(rows, "minecraft:golden_apple").startsWith("minecraft:golden_apple,minecraft,false,4,9.60,\"minecraft:regeneration,100,1,1.00;minecraft:absorption,2400,0,1.00\",true,"),
                "golden apple row: " + row(rows, "minecraft:golden_apple"));
        assertTrue(helper, row(rows, "minecraft:oak_log").contains("minecraft:logs"), "tags of the oak log");

        List<String> all = ItemDump.rows(Set.of(ItemDump.ALL));
        assertFalse(helper, row(all, "minecraft:apple").isEmpty(), "* leaves out minecraft");
        assertFalse(helper, row(all, "droplets_of_thirst:terracotta_water_bowl").isEmpty(), "* leaves out droplets_of_thirst");
        helper.succeed();
    }

    private static String row(List<String> rows, String id)
    {
        return rows.stream().filter(row -> row.startsWith(id + ",")).findFirst().orElse("");
    }
}
