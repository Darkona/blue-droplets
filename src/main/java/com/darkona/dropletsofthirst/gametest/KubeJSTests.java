package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.api.DropletsAPI;
import com.darkona.dropletsofthirst.api.PurityLevel;
import com.darkona.dropletsofthirst.api.ThirstValues;
import com.darkona.dropletsofthirst.api.event.ThirstChangeEvent;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static com.darkona.dropletsofthirst.gametest.TestSupport.player;
import static com.darkona.dropletsofthirst.gametest.TestSupport.thirst;

/**
 * The scripts of {@code examples/kubejs}, running inside KubeJS. Registered by {@link DropletsGameTests} only when
 * KubeJS is installed; {@code -PwithKubeJS} copies the scripts into {@code run/kubejs} before the run.
 */
@PrefixGameTestTemplate(false)
public class KubeJSTests
{
    private static final String HINT = " (are the scripts of examples/kubejs in run/kubejs? use -PwithKubeJS)";

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void startupScriptRegistersADrink(GameTestHelper helper)
    {
        Item lemonade = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("kubejs", "lemonade"));
        helper.assertTrue(lemonade != net.minecraft.world.item.Items.AIR, "kubejs:lemonade does not exist" + HINT);
        ThirstValues values = DropletsAPI.getDrinkValues(new ItemStack(lemonade));
        helper.assertTrue(values != null, "kubejs:lemonade has no thirst values" + HINT);
        helper.assertValueEqual(values.thirst(), 6, "lemonade thirst");
        helper.assertValueEqual(values.quenched(), 4, "lemonade quenched");
        helper.assertValueEqual(values.purity(), PurityLevel.CLEAN.level(), "lemonade purity");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void pureWaterRefreshesMoreForTaggedPlayersThroughAScriptListener(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        thirst(player).change(player, 10, 0, ThirstChangeEvent.Cause.COMMAND);
        DropletsAPI.drink(player, 4, 2, PurityLevel.PURE.level());
        helper.assertValueEqual(DropletsAPI.view(player).quenched(), 2, "quenched without the tag");
        player.addTag("hydro");
        DropletsAPI.drink(player, 4, 2, PurityLevel.PURE.level());
        helper.assertValueEqual(DropletsAPI.view(player).thirst(), 18, "thirst after two drinks");
        helper.assertValueEqual(DropletsAPI.view(player).quenched(), 6, "quenched with the tag (2 and 2 from the script)" + HINT);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void dirtyWaterIsCancelledInTheNether(GameTestHelper helper)
    {
        ServerPlayer nether = FakePlayerFactory.get(helper.getLevel().getServer().getLevel(Level.NETHER),
                new GameProfile(UUID.randomUUID(), "droplets-test-nether"));
        thirst(nether).change(nether, 10, 0, ThirstChangeEvent.Cause.COMMAND);
        boolean hydrated = DropletsAPI.drink(nether, 4, 2, PurityLevel.CONTAMINATED.level());
        helper.assertFalse(hydrated, "contaminated water hydrated in the Nether" + HINT);
        helper.assertValueEqual(DropletsAPI.view(nether).thirst(), 10, "thirst after the drink");

        ServerPlayer overworld = player(helper);
        thirst(overworld).change(overworld, 10, 0, ThirstChangeEvent.Cause.COMMAND);
        DropletsAPI.drink(overworld, 4, 2, PurityLevel.CLEAN.level());
        helper.assertValueEqual(DropletsAPI.view(overworld).thirst(), 14, "thirst after clean water in the overworld");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void hubTagStopsThirstLossThroughAScriptListener(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        thirst(player).change(player, 10, 0, ThirstChangeEvent.Cause.COMMAND);
        player.addTag("hub");
        DropletsAPI.addExhaustion(player, 40f);
        thirst(player).tick(player);
        helper.assertValueEqual(DropletsAPI.view(player).thirst(), 10, "thirst in the hub" + HINT);
        player.removeTag("hub");
        thirst(player).tick(player);
        helper.assertValueEqual(DropletsAPI.view(player).thirst(), 9, "thirst outside the hub");
        helper.succeed();
    }

    private static void rightClick(ServerPlayer player, Item item)
    {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND));
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void thirstScriptsReadChangeAndHydrate(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        thirst(player).change(player, 10, 0, ThirstChangeEvent.Cause.COMMAND);

        // A golden carrot hydrates what Droplets of Thirst says, and 4 more (drink() from the script)
        ItemStack carrot = new ItemStack(Items.GOLDEN_CARROT);
        ThirstValues own = DropletsAPI.getDrinkValues(carrot);
        int expected = 14 + (own == null ? 0 : own.thirst());
        player.eat(helper.getLevel(), carrot);
        helper.assertValueEqual(DropletsAPI.view(player).thirst(), expected, "thirst after the golden carrot" + HINT);

        // The sponge dries 2 points
        rightClick(player, Items.SPONGE);
        helper.assertValueEqual(DropletsAPI.view(player).thirst(), expected - 2, "thirst after the sponge" + HINT);

        // The clock refills when sneaking, and only reads otherwise
        rightClick(player, Items.CLOCK);
        helper.assertValueEqual(DropletsAPI.view(player).thirst(), expected - 2, "thirst after reading the clock");
        player.setShiftKeyDown(true);
        rightClick(player, Items.CLOCK);
        helper.assertValueEqual(DropletsAPI.view(player).thirst(), 20, "thirst after refilling with the clock");
        helper.assertValueEqual(DropletsAPI.view(player).quenched(), 20, "quenched after refilling with the clock");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void purityScriptsReadAndWrite(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        // The glass bottle gives a pure water bottle
        rightClick(player, Items.GLASS_BOTTLE);
        boolean found = false;
        for (ItemStack stack : player.getInventory().items)
            if (stack.is(Items.POTION) && DropletsAPI.getPurity(stack) == PurityLevel.PURE.level())
                found = true;
        helper.assertTrue(found, "no pure water bottle after the glass bottle" + HINT);
        // The water bottle read (the script only tells; an error would show in kubejs/logs/server.log)
        rightClick(player, Items.POTION);
        helper.succeed();
    }
}
