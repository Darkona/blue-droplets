package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.api.DropletsAPI;
import com.darkona.dropletsofthirst.api.PurityLevel;
import com.darkona.dropletsofthirst.api.event.DrinkEvent;
import com.darkona.dropletsofthirst.api.event.ThirstChangeEvent;
import com.darkona.dropletsofthirst.compat.vampirism.VampirismCompat;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.registry.EffectInit;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.content.thirst.ThirstHelper;
import com.darkona.dropletsofthirst.foundation.config.GameplayConfig;
import com.darkona.dropletsofthirst.foundation.gui.ThirstBarStyles;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.entity.player.vampire.VampirePlayer;
import de.teamlapen.vampirism.entity.vampire.DrinkBloodContext;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.darkona.dropletsofthirst.gametest.TestSupport.player;
import static com.darkona.dropletsofthirst.gametest.TestSupport.thirst;
import static com.darkona.dropletsofthirst.gametest.TestSupport.assertValueEqual;

/**
 * Vampirism ({@code -PwithVampirism}): a vampire loses thirst like anyone, only blood hydrates it, and every drink of
 * blood goes through Vampirism's own path ({@code VampirePlayer#drinkBlood}, which posts its event). Registered by
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
        helper.assertTrue(VampirismCompat.isVampire(player), "the test player did not become a vampire");
        return player;
    }

    private static void drinkBlood(ServerPlayer player, int blood, float saturation, DrinkBloodContext source)
    {
        VampirePlayer.get(player).drinkBlood(blood, saturation, false, source);
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
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

    /** Quenchness is not blood: it gives a vampire nothing. */
    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void quenchnessDoesNotHydrateAVampire(GameTestHelper helper)
    {
        ServerPlayer player = vampire(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 10, 0, ThirstChangeEvent.Cause.COMMAND);
        EffectInit.QUENCHNESS.get().applyEffectTick(player, 1);
        assertValueEqual(helper, thirst.getThirst(), 10, "a vampire's thirst after a Quenchness tick");
        assertValueEqual(helper, thirst.getQuenched(), 0, "a vampire's quenched after a Quenchness tick");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void onlyBloodHydratesAVampire(GameTestHelper helper)
    {
        ServerPlayer player = vampire(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 10, 0, ThirstChangeEvent.Cause.COMMAND);
        ItemStack water = WaterPurity.addPurity(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), PurityLevel.PURE.level());
        helper.assertFalse(ThirstHelper.playerRestoresThirst(water, player), "water restores a vampire's thirst");
        PlayerThirst.consume(water, player);
        PlayerThirst.consume(new ItemStack(Items.MELON_SLICE), player);
        helper.assertFalse(PlayerThirst.drink(player, ItemStack.EMPTY, 1, 1, PurityLevel.PURE.level()), "drinking by hand hydrated a vampire");
        helper.assertFalse(DropletsAPI.drink(player, 4, 4), "DropletsAPI.drink hydrated a vampire");
        assertValueEqual(helper, thirst.getThirst(), 10, "a vampire's thirst after water, a melon slice and a sip by hand");
        assertValueEqual(helper, thirst.getQuenched(), 0, "a vampire's quenched after water, a melon slice and a sip by hand");
        helper.assertTrue(player.getActiveEffects().isEmpty(), "water gave a vampire effects: " + player.getActiveEffects());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void bloodBottleHydratesAVampire(GameTestHelper helper)
    {
        ServerPlayer player = vampire(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 6, 0, ThirstChangeEvent.Cause.COMMAND);
        ItemStack bottle = new ItemStack(ModItems.BLOOD_BOTTLE.get());
        lastPurity = PurityLevel.MIN;
        drinkBlood(player, 9, 0.45F, new DrinkBloodContext(bottle));
        assertValueEqual(helper, thirst.getThirst(), 15, "thirst after 9 blood from a bottle");
        assertValueEqual(helper, thirst.getQuenched(), 4, "quenched after 9 blood at saturation 0.45");
        assertValueEqual(helper, lastPurity, DropletsAPI.NO_PURITY, "purity of a drink of blood");
        drinkBlood(player, 3, 0.45F, new DrinkBloodContext(bottle));
        assertValueEqual(helper, thirst.getQuenched(), 5, "quenched after 3 more blood (0.05 + 1.35): the fractions carry over");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void biteHydratesAVampire(GameTestHelper helper)
    {
        ServerPlayer player = vampire(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(1, 1, 1));
        drinkBlood(player, 5, 0.7F, new DrinkBloodContext(cow));
        assertValueEqual(helper, thirst.getThirst(), 9, "thirst after biting 5 blood");
        assertValueEqual(helper, thirst.getQuenched(), 3, "quenched after biting 5 blood at saturation 0.7");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void fillingTheBloodBarIsNotDrinking(GameTestHelper helper)
    {
        ServerPlayer player = vampire(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        drinkBlood(player, Integer.MAX_VALUE, 0.0F, DrinkBloodContext.none());
        assertValueEqual(helper, thirst.getThirst(), 4, "thirst after an altar or a command filled the blood bar");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void vampireBarIsRed(GameTestHelper helper)
    {
        VampirismCompat.initClient();
        ServerPlayer human = player(helper);
        helper.assertTrue(ThirstBarStyles.activeStyle(human) == null, "a human has the bar style " + ThirstBarStyles.activeStyle(human));
        assertValueEqual(helper, ThirstBarStyles.activeStyle(vampire(helper)), DropletsOfThirst.asResource("vampirism_vampire"), "bar style of a vampire");
        helper.succeed();
    }
}
