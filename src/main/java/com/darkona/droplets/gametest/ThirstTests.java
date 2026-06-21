package com.darkona.droplets.gametest;

import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.event.ThirstChangeEvent;
import com.darkona.droplets.content.data.DimensionWater;
import com.darkona.droplets.content.data.DropletsDataMaps;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.content.registry.EffectInit;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static com.darkona.droplets.gametest.TestSupport.player;
import static com.darkona.droplets.gametest.TestSupport.thirst;

/**
 * Drinking, eating, limits, overhydration, purity effects and the commands, with the default config.
 */
@GameTestHolder(BlueDroplets.ID)
@PrefixGameTestTemplate(false)
public class ThirstTests
{
    /** Cause of the last thirst change of any player; tests run one at a time on the server thread. */
    private static ThirstChangeEvent.Cause lastCause;

    static
    {
        NeoForge.EVENT_BUS.addListener((ThirstChangeEvent.Post event) -> lastCause = event.getCause());
    }

    private static ItemStack waterBottle(int purity)
    {
        return WaterPurity.addPurity(PotionContents.createItemStack(Items.POTION, Potions.WATER), purity);
    }

    @GameTest(template = "empty")
    public static void waterBottleIsDrunk(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        PlayerThirst.consume(waterBottle(WaterPurity.MAX_PURITY), player);
        helper.assertValueEqual(thirst.getThirst(), 10, "thirst after a purified water bottle (4 + 2 purified)");
        helper.assertValueEqual(thirst.getQuenched(), 8, "quenched after a purified water bottle (5 + 3 purified)");
        helper.assertValueEqual(lastCause, ThirstChangeEvent.Cause.DRINK, "cause of drinking a water bottle");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyPurifiedWaterGetsTheBonus(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        PlayerThirst.consume(waterBottle(2), player);
        helper.assertValueEqual(thirst.getThirst(), 8, "thirst after an acceptable water bottle (4)");
        helper.assertValueEqual(thirst.getQuenched(), 5, "quenched after an acceptable water bottle (5)");

        ThirstValues purified = ThirstHelper.drinkValuesOf(waterBottle(WaterPurity.MAX_PURITY));
        helper.assertTrue(purified != null && purified.thirst() == 6 && purified.quenched() == 8, "values shown for a purified water bottle: " + purified);
        helper.assertTrue(ThirstHelper.drinkValuesOf(waterBottle(WaterPurity.MAX_PURITY)) == purified, "the purified values are built again for the same stack");
        ThirstValues acceptable = ThirstHelper.drinkValuesOf(waterBottle(2));
        helper.assertTrue(acceptable != null && acceptable.thirst() == 4 && acceptable.quenched() == 5, "values shown for an acceptable water bottle: " + acceptable);

        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        PlayerThirst.drinkWater(player, 3, 2, WaterPurity.MAX_PURITY);
        helper.assertValueEqual(thirst.getThirst(), 9, "thirst after a sip of purified water by hand (3 + 2)");
        helper.assertValueEqual(thirst.getQuenched(), 5, "quenched after a sip of purified water by hand (2 + 3)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkingABowlGivesTheEmptyBowlBack(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        ItemStack bowls = new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get(), 2);
        ItemStack left = bowls.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(left.is(ItemInit.TERRACOTTA_WATER_BOWL.get()) && left.getCount() == 1, "water bowls left after drinking one of two");
        helper.assertValueEqual(player.getInventory().countItem(ItemInit.TERRACOTTA_BOWL.get()), 1, "empty bowls in the inventory");
        ItemStack last = left.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(last.is(ItemInit.TERRACOTTA_BOWL.get()) && last.getCount() == 1, "drinking the last water bowl leaves the empty bowl in hand");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void appleIsEaten(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        PlayerThirst.consume(new ItemStack(Items.APPLE), player);
        helper.assertValueEqual(thirst.getThirst(), 6, "thirst after an apple (2)");
        helper.assertValueEqual(lastCause, ThirstChangeEvent.Cause.EAT, "cause of eating an apple");
        helper.assertFalse(player.hasEffect(MobEffects.CONFUSION), "food rolled purity effects");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void quenchedNeverExceedsThirst(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 10, ThirstChangeEvent.Cause.COMMAND);
        helper.assertValueEqual(thirst.getQuenched(), 4, "quenched set above thirst");
        thirst.change(player, 2, 2, ThirstChangeEvent.Cause.COMMAND);
        helper.assertValueEqual(thirst.getQuenched(), 2, "quenched after thirst went down");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void saltyValuesStopAtZero(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 1, 1, ThirstChangeEvent.Cause.COMMAND);
        thirst.hydrate(player, -6, -6, true, ThirstChangeEvent.Cause.EAT);
        helper.assertValueEqual(thirst.getThirst(), 0, "thirst after salty food");
        helper.assertValueEqual(thirst.getQuenched(), 0, "quenched after salty food");
        helper.assertFalse(player.hasEffect(EffectInit.OVERHYDRATED), "salty food overhydrated");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkingFarPastFullOverhydrates(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 20, 20, ThirstChangeEvent.Cause.COMMAND);
        thirst.hydrate(player, 6, 8, true, ThirstChangeEvent.Cause.DRINK);
        helper.assertFalse(player.hasEffect(EffectInit.OVERHYDRATED), "one bottle past full already overhydrated");
        thirst.hydrate(player, 6, 8, true, ThirstChangeEvent.Cause.DRINK);
        MobEffectInstance effect = player.getEffect(EffectInit.OVERHYDRATED);
        helper.assertTrue(effect != null && effect.getAmplifier() == 0, "two bottles past full give Overhydrated I");
        helper.assertTrue(player.hasEffect(MobEffects.CONFUSION), "Overhydrated comes with Nausea");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invulnerablePlayersDoNotOverhydrate(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        player.getAbilities().invulnerable = true;
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 20, 20, ThirstChangeEvent.Cause.COMMAND);
        for (int i = 0; i < 4; i++)
            thirst.hydrate(player, 6, 8, true, ThirstChangeEvent.Cause.DRINK);
        helper.assertFalse(player.hasEffect(EffectInit.OVERHYDRATED), "an invulnerable player overhydrated");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dirtyWaterGivesItsEffects(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst.consume(waterBottle(0), player);
        helper.assertTrue(player.hasEffect(MobEffects.CONFUSION), "dirty water gives Nausea (100% by default)");
        helper.assertTrue(player.hasEffect(MobEffects.HUNGER), "dirty water gives Hunger (100% by default)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purifiedWaterGivesNoEffects(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst.consume(waterBottle(WaterPurity.MAX_PURITY), player);
        helper.assertTrue(player.getActiveEffects().isEmpty(), "purified water gave " + player.getActiveEffects());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theEndIsCold(GameTestHelper helper)
    {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "no End");
        ServerPlayer player = FakePlayerFactory.get(end, new GameProfile(UUID.randomUUID(), "droplets-test"));
        player.moveTo(0.5, 64, 0.5);
        DimensionWater water = end.dimensionTypeRegistration().getData(DropletsDataMaps.DIMENSION_WATER);
        helper.assertTrue(water != null && water.thirstMultiplier().isPresent(), "the End has no thirst_multiplier");
        float climate = ThirstHelper.getExhaustionBiomeModifier(player);
        helper.assertValueEqual(climate, water.thirstMultiplier().get(), "climate multiplier in the End");
        helper.assertTrue(climate < 1.0F, "climate multiplier in the End " + climate + ", not below 1");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commandsReturnTheirResult(GameTestHelper helper) throws Exception
    {
        ServerPlayer player = player(helper);
        CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        helper.assertValueEqual(dispatcher.execute("blue_droplets set @s 4 10", source), 4, "result of set");
        helper.assertValueEqual(thirst(player).getQuenched(), 4, "quenched after set 4 10");
        helper.assertValueEqual(dispatcher.execute("blue_droplets query @s", source), 4, "result of query");
        helper.assertValueEqual(dispatcher.execute("thirst query @s", source), 4, "result of the /thirst alias");
        helper.assertValueEqual(dispatcher.execute("blue_droplets enable @s false", source), 1, "result of enable");
        helper.assertFalse(thirst(player).getShouldTickThirst(), "thirst still enabled after enable false");
        helper.succeed();
    }
}
