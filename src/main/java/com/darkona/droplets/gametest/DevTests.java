package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.foundation.dev.ItemDump;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

/**
 * Development tools: the item dump behind {@code /blue_droplets dev dump_items}, which only exists outside production.
 */
@GameTestHolder(BlueDroplets.ID)
@PrefixGameTestTemplate(false)
public class DevTests
{
    @GameTest(template = "empty")
    public static void itemDumpListsFoodDrinksAndCurrentValues(GameTestHelper helper)
    {
        helper.assertValueEqual(helper.getLevel().getServer().getCommands().getDispatcher().getRoot()
                .getChild(BlueDroplets.ID).getChild("dev") != null, !FMLEnvironment.production, "dev commands registered outside production");

        List<String> rows = ItemDump.rows(Set.of("minecraft"));
        helper.assertValueEqual(rows.get(0), ItemDump.HEADER, "header");
        helper.assertTrue(rows.stream().allMatch(row -> row.startsWith("minecraft:") || row == rows.get(0)), "only the asked namespace");
        String potion = row(rows, "minecraft:potion");
        helper.assertTrue(potion.startsWith("minecraft:potion,minecraft,true,,,,," + ThirstHelper.getThirst(Items.POTION.getDefaultInstance())
                + "," + ThirstHelper.getQuenched(Items.POTION.getDefaultInstance()) + ","), "water bottle row: " + potion);
        helper.assertTrue(row(rows, "minecraft:golden_apple").startsWith("minecraft:golden_apple,minecraft,false,4,9.60,\"minecraft:regeneration,100,1,1.00;minecraft:absorption,2400,0,1.00\",true,"),
                "golden apple row: " + row(rows, "minecraft:golden_apple"));
        helper.assertTrue(row(rows, "minecraft:oak_log").contains("minecraft:logs"), "tags of the oak log");
        helper.succeed();
    }

    private static String row(List<String> rows, String id)
    {
        return rows.stream().filter(row -> row.startsWith(id + ",")).findFirst().orElse("");
    }
}
