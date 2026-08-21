package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.DropletsAPI;
import com.darkona.droplets.api.PurityLevel;
import com.darkona.droplets.api.event.DrinkEvent;
import com.darkona.droplets.api.event.ThirstChangeEvent;
import com.darkona.droplets.compat.vampirism.VampirismCompat;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.foundation.gui.ThirstBarStyles;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.player.vampire.VampirePlayer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.darkona.droplets.gametest.TestSupport.assertFalse;
import static com.darkona.droplets.gametest.TestSupport.assertTrue;
import static com.darkona.droplets.gametest.TestSupport.assertValueEqual;
import static com.darkona.droplets.gametest.TestSupport.player;
import static com.darkona.droplets.gametest.TestSupport.thirst;

/**
 * Vampirism ({@code -PwithVampirism}): a vampire loses thirst like anyone, only blood hydrates it, and every drink of
 * blood goes through Vampirism's own {@code VampirePlayer#drinkBlood} (the mixin hook of this version). Registered by
 * {@link DropletsGameTests} only when Vampirism is installed.
 */
@PrefixGameTestTemplate(false)
public class VampirismTests
{
    /** Purity of the last drink of any player; tests run one at a time on the server thread. */
    private static int lastPurity;

    static
    {
        MinecraftForge.EVENT_BUS.addListener((DrinkEvent.Post event) -> lastPurity = event.getPurity());
    }

    private static ServerPlayer vampire(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        FactionPlayerHandler.get(player).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
        assertTrue(helper, VampirismCompat.isVampire(player), "the test player did not become a vampire");
        return player;
    }

    private static void drinkBlood(ServerPlayer player, int blood, float saturation)
    {
        VampirePlayer.get(player).drinkBlood(blood, saturation, false);
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void vampireThirstDrops(GameTestHelper helper)
    {
        ServerPlayer player = vampire(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 20, 0, ThirstChangeEvent.Cause.COMMAND);
        thirst.setExhaustion(GameplayConfig.EXHAUSTION_PER_POINT.get().floatValue() + 0.5F);
        thirst.tick(player);
        assertValueEqual(helper, thirst.getThirst(), 19, "a vampire's thirst after a point of exhaustion");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void onlyBloodHydratesAVampire(GameTestHelper helper)
    {
        ServerPlayer player = vampire(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 10, 0, ThirstChangeEvent.Cause.COMMAND);
        ItemStack water = WaterPurity.addPurity(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), PurityLevel.PURE.level());
        assertFalse(helper, ThirstHelper.playerRestoresThirst(water, player), "water restores a vampire's thirst");
        PlayerThirst.consume(water, player);
        PlayerThirst.consume(new ItemStack(Items.MELON_SLICE), player);
        assertFalse(helper, PlayerThirst.drink(player, ItemStack.EMPTY, 1, 1, PurityLevel.PURE.level()), "drinking by hand hydrated a vampire");
        assertFalse(helper, DropletsAPI.drink(player, 4, 4), "DropletsAPI.drink hydrated a vampire");
        assertValueEqual(helper, thirst.getThirst(), 10, "a vampire's thirst after water, a melon slice and a sip by hand");
        assertValueEqual(helper, thirst.getQuenched(), 0, "a vampire's quenched after water, a melon slice and a sip by hand");
        assertTrue(helper, player.getActiveEffects().isEmpty(), "water gave a vampire effects: " + player.getActiveEffects());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void drinkingBloodHydratesAVampire(GameTestHelper helper)
    {
        ServerPlayer player = vampire(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 6, 0, ThirstChangeEvent.Cause.COMMAND);
        lastPurity = PurityLevel.MIN;
        drinkBlood(player, 9, 0.3F);
        assertValueEqual(helper, thirst.getThirst(), 15, "thirst after 9 blood");
        assertValueEqual(helper, thirst.getQuenched(), 2, "quenched after 9 blood at saturation 0.3");
        assertValueEqual(helper, lastPurity, DropletsAPI.NO_PURITY, "purity of a drink of blood");
        drinkBlood(player, 3, 0.3F);
        assertValueEqual(helper, thirst.getQuenched(), 3, "quenched after 3 more blood: the fractions carry over");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void fillingTheBloodBarIsNotDrinking(GameTestHelper helper)
    {
        ServerPlayer player = vampire(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        drinkBlood(player, Integer.MAX_VALUE, 0.0F);
        assertValueEqual(helper, thirst.getThirst(), 4, "thirst after an altar or a command filled the blood bar");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void vampireBarIsRed(GameTestHelper helper)
    {
        VampirismCompat.initClient();
        ServerPlayer human = player(helper);
        assertTrue(helper, ThirstBarStyles.activeStyle(human) == null, "a human has the bar style " + ThirstBarStyles.activeStyle(human));
        assertValueEqual(helper, ThirstBarStyles.activeStyle(vampire(helper)), BlueDroplets.asResource("vampirism_vampire"), "bar style of a vampire");
        helper.succeed();
    }
}
