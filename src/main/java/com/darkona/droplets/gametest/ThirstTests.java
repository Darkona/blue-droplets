package com.darkona.droplets.gametest;

import net.minecraft.world.level.biome.Biomes;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.darkona.droplets.content.thirst.PlayerThirstManager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Direction;
import com.darkona.droplets.api.ThirstValues;
import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.event.ThirstChangeEvent;
import com.darkona.droplets.content.data.DimensionWater;
import com.darkona.droplets.content.data.DropletsDataMaps;
import com.darkona.droplets.api.PurityLevel;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.PurityConfig;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.content.registry.EffectInit;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.foundation.config.GameplayConfig;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.mojang.authlib.GameProfile;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;

import java.util.UUID;

import static com.darkona.droplets.gametest.TestSupport.player;
import static com.darkona.droplets.gametest.TestSupport.thirst;
import static com.darkona.droplets.gametest.TestSupport.assertValueEqual;
import static com.darkona.droplets.gametest.TestSupport.assertTrue;
import static com.darkona.droplets.gametest.TestSupport.assertFalse;
import static com.darkona.droplets.gametest.TestSupport.set;

/**
 * Drinking, eating, limits, overhydration, purity effects and the commands, with the default config.
 */
@GameTestHolder(BlueDroplets.ID)
@PrefixGameTestTemplate(false)
public class ThirstTests
{
    /** Cause of the last thirst change of any player; tests run one at a time on the server thread. */
    private static ThirstChangeEvent.Cause lastCause;
    /** Players with this tag do not die: their death is cancelled after every other listener, like a modded totem. */
    private static final String IMMORTAL_TAG = "droplets-test-immortal";
    /** Players with this tag cannot break blocks nor attack: cancelled at low priority, like a claim or PvP mod. */
    private static final String PROTECTED_TAG = "droplets-test-protected";

    static
    {
        MinecraftForge.EVENT_BUS.addListener((ThirstChangeEvent.Post event) -> lastCause = event.getCause());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> {
            if (event.getEntity().getTags().contains(IMMORTAL_TAG))
                event.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, (BlockEvent.BreakEvent event) -> {
            if (event.getPlayer().getTags().contains(PROTECTED_TAG))
                event.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, (AttackEntityEvent event) -> {
            if (event.getEntity().getTags().contains(PROTECTED_TAG))
                event.setCanceled(true);
        });
    }

    private static ItemStack waterBottle(int purity)
    {
        return WaterPurity.addPurity(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), purity);
    }

    /** MIRROR_FOOD: hunger exhaustion that another mod empties between ticks (Vampirism, for its vampires) still counts. */
    @GameTest(template = "empty")
    public static void emptiedHungerExhaustionStillCounts(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        double basal = GameplayConfig.BASAL_PER_TICK.get();
        try
        {
            set(GameplayConfig.BASAL_PER_TICK, 0.0);
            FoodData food = player.getFoodData();
            food.setExhaustion(0.0F);
            thirst.tick(player);
            float start = thirst.getExhaustion();
            food.setExhaustion(0.5F);
            thirst.tick(player);
            float first = thirst.getExhaustion();
            assertTrue(helper, first > start, "hunger exhaustion did not add thirst exhaustion");
            food.setExhaustion(0.25F);
            thirst.tick(player);
            assertTrue(helper, thirst.getExhaustion() > first, "thirst exhaustion went from " + first + " to " + thirst.getExhaustion() + " after the hunger exhaustion was emptied and grew again");
        }
        finally
        {
            set(GameplayConfig.BASAL_PER_TICK, basal);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void waterBottleIsDrunk(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        PlayerThirst.consume(waterBottle(PurityLevel.PURE.level()), player);
        assertValueEqual(helper, thirst.getThirst(), 10, "thirst after a pure water bottle (4 + 2 pure)");
        assertValueEqual(helper, thirst.getQuenched(), 8, "quenched after a pure water bottle (5 + 3 pure)");
        assertValueEqual(helper, lastCause, ThirstChangeEvent.Cause.DRINK, "cause of drinking a water bottle");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyPureWaterGetsTheBonus(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        int pure = PurityLevel.PURE.level();
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        PlayerThirst.consume(waterBottle(PurityLevel.CLEAN.level()), player);
        assertValueEqual(helper, thirst.getThirst(), 8, "thirst after a clean water bottle (4)");
        assertValueEqual(helper, thirst.getQuenched(), 5, "quenched after a clean water bottle (5)");

        ThirstValues values = ThirstHelper.drinkValuesOf(waterBottle(pure));
        assertTrue(helper, values != null && values.thirst() == 6 && values.quenched() == 8, "values shown for a pure water bottle: " + values);
        assertTrue(helper, ThirstHelper.drinkValuesOf(waterBottle(pure)) == values, "the pure values are built again for the same stack");
        for (int purity = PurityLevel.MIN; purity < pure; purity++)
        {
            ThirstValues other = ThirstHelper.drinkValuesOf(waterBottle(purity));
            assertTrue(helper, other != null && other.thirst() == 4 && other.quenched() == 5, "values shown for a water bottle of purity " + purity + ": " + other);
        }

        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        PlayerThirst.drinkWater(player, 3, 2, pure);
        assertValueEqual(helper, thirst.getThirst(), 9, "thirst after a hose sip of pure water (3 + 2)");
        assertValueEqual(helper, thirst.getQuenched(), 5, "quenched after a hose sip of pure water (2 + 3)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkingABowlGivesTheEmptyBowlBack(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        ItemStack bowls = new ItemStack(ItemInit.TERRACOTTA_WATER_BOWL.get(), 2);
        ItemStack left = bowls.finishUsingItem(helper.getLevel(), player);
        assertTrue(helper, left.is(ItemInit.TERRACOTTA_WATER_BOWL.get()) && left.getCount() == 1, "water bowls left after drinking one of two");
        assertValueEqual(helper, player.getInventory().countItem(ItemInit.TERRACOTTA_BOWL.get()), 1, "empty bowls in the inventory");
        ItemStack last = left.finishUsingItem(helper.getLevel(), player);
        assertTrue(helper, last.is(ItemInit.TERRACOTTA_BOWL.get()) && last.getCount() == 1, "drinking the last water bowl leaves the empty bowl in hand");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void appleIsEaten(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        PlayerThirst.consume(new ItemStack(Items.APPLE), player);
        assertValueEqual(helper, thirst.getThirst(), 6, "thirst after an apple (2)");
        assertValueEqual(helper, lastCause, ThirstChangeEvent.Cause.EAT, "cause of eating an apple");
        assertFalse(helper, player.hasEffect(MobEffects.CONFUSION), "food rolled purity effects");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void quenchedNeverExceedsThirst(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 10, ThirstChangeEvent.Cause.COMMAND);
        assertValueEqual(helper, thirst.getQuenched(), 4, "quenched set above thirst");
        thirst.change(player, 2, 2, ThirstChangeEvent.Cause.COMMAND);
        assertValueEqual(helper, thirst.getQuenched(), 2, "quenched after thirst went down");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void saltyValuesStopAtZero(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 1, 1, ThirstChangeEvent.Cause.COMMAND);
        thirst.hydrate(player, -6, -6, true, ThirstChangeEvent.Cause.EAT);
        assertValueEqual(helper, thirst.getThirst(), 0, "thirst after salty food");
        assertValueEqual(helper, thirst.getQuenched(), 0, "quenched after salty food");
        assertFalse(helper, player.hasEffect(EffectInit.OVERHYDRATED.get()), "salty food overhydrated");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkingFarPastFullOverhydrates(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 20, 20, ThirstChangeEvent.Cause.COMMAND);
        thirst.hydrate(player, 6, 8, true, ThirstChangeEvent.Cause.DRINK);
        assertFalse(helper, player.hasEffect(EffectInit.OVERHYDRATED.get()), "one bottle past full already overhydrated");
        thirst.hydrate(player, 6, 8, true, ThirstChangeEvent.Cause.DRINK);
        MobEffectInstance effect = player.getEffect(EffectInit.OVERHYDRATED.get());
        assertTrue(helper, effect != null && effect.getAmplifier() == 0, "two bottles past full give Overhydrated I");
        assertTrue(helper, player.hasEffect(MobEffects.CONFUSION), "Overhydrated comes with Nausea");
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
        assertFalse(helper, player.hasEffect(EffectInit.OVERHYDRATED.get()), "an invulnerable player overhydrated");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dirtyWaterGivesItsEffects(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst.consume(waterBottle(0), player);
        assertTrue(helper, player.hasEffect(MobEffects.CONFUSION), "dirty water gives Nausea (100% by default)");
        assertTrue(helper, player.hasEffect(MobEffects.HUNGER), "dirty water gives Hunger (100% by default)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cleanAndPureWaterGiveNoEffects(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst.consume(waterBottle(PurityLevel.CLEAN.level()), player);
        assertTrue(helper, player.getActiveEffects().isEmpty(), "clean water gave " + player.getActiveEffects());
        PlayerThirst.consume(waterBottle(PurityLevel.PURE.level()), player);
        assertTrue(helper, player.getActiveEffects().isEmpty(), "pure water gave " + player.getActiveEffects());
        helper.succeed();
    }

    /** Default {@code [effects]}: contaminated and dirty water can poison, murky and acceptable only sicken, clean and pure do nothing. */
    @GameTest(template = "empty")
    public static void defaultEffectTablesFollowTheSixLevels(GameTestHelper helper)
    {
        double[][] chances = {{100, 100, 40}, {60, 60, 15}, {25, 25}, {5, 5}, {}, {}};
        assertValueEqual(helper, PurityConfig.EFFECTS.size(), PurityLevel.values().length, "effect lists");
        for (PurityLevel level : PurityLevel.values())
        {
            WaterPurity.PurityEffect[] table = WaterPurity.effectTable(level.level());
            double[] expected = chances[level.level()];
            assertValueEqual(helper, table.length, expected.length, "effects of " + level.id());
            for (int i = 0; i < table.length; i++)
            {
                assertTrue(helper, Math.abs(table[i].chance() * 100 - expected[i]) < 0.001, level.id() + " effect " + i + " chance " + table[i].chance());
                assertValueEqual(helper, table[i].blocksHydration(), i == 2, level.id() + " effect " + i + " blocks hydration");
            }
        }
        assertValueEqual(helper, ((ForgeConfigSpec.ValueSpec) PurityConfig.SPEC.getSpec().get(PurityConfig.HOT_DIRTY_WATER_MAX_PURITY.getPath())).getDefault(), PurityLevel.MURKY.level(), "hotDirtyWater.maxPurity default");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void climateFallsWithAltitude(GameTestHelper helper)
    {
        Level level = helper.getLevel();
        BlockPos low = helper.absolutePos(BlockPos.ZERO).atY(level.getSeaLevel());
        BlockPos high = low.above(200);
        // Plains, not the biome where the test runs: in a hot biome the LEGACY formula halves temperatures above 1, so
        // cooling with height can raise the multiplier there.
        var plains = level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getHolderOrThrow(Biomes.PLAINS);
        float atSea = ThirstHelper.biomeClimate(level, plains, low, 1.0F, false);
        float onTop = ThirstHelper.biomeClimate(level, plains, high, 1.0F, false);
        assertTrue(helper, onTop < atSea, "climate multiplier 200 blocks above sea level " + onTop + ", not below " + atSea);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theEndIsCold(GameTestHelper helper)
    {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        assertTrue(helper, end != null, "no End");
        ServerPlayer player = FakePlayerFactory.get(end, new GameProfile(UUID.randomUUID(), "droplets-test"));
        player.moveTo(0.5, 64, 0.5);
        DimensionWater water = DropletsDataMaps.DIMENSION_WATER.get(end.dimensionTypeRegistration());
        assertTrue(helper, water != null && water.thirstMultiplier().isPresent(), "the End has no thirst_multiplier");
        float climate = ThirstHelper.getExhaustionBiomeModifier(player);
        assertValueEqual(helper, climate, water.thirstMultiplier().get(), "climate multiplier in the End");
        assertTrue(helper, climate < 1.0F, "climate multiplier in the End " + climate + ", not below 1");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commandsReturnTheirResult(GameTestHelper helper) throws Exception
    {
        ServerPlayer player = player(helper);
        CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        assertValueEqual(helper, dispatcher.execute("blue_droplets set @s 4 10", source), 4, "result of set");
        assertValueEqual(helper, thirst(player).getQuenched(), 4, "quenched after set 4 10");
        assertValueEqual(helper, dispatcher.execute("blue_droplets query @s", source), 4, "result of query");
        assertValueEqual(helper, dispatcher.execute("thirst query @s", source), 4, "result of the /thirst alias");
        assertValueEqual(helper, dispatcher.execute("blue_droplets enable @s false", source), 1, "result of enable");
        assertFalse(helper, thirst(player).getShouldTickThirst(), "thirst still enabled after enable false");
        helper.succeed();
    }

    /** Gaining or losing an effect recomputes the thirst loss multiplier right away (Fire Resistance: none by default). */
    @GameTest(template = "empty")
    public static void effectsRecomputeTheMultiplier(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        float plain = thirst.exhaustionModifier(player);
        assertTrue(helper, plain > 0.0F, "multiplier without effects " + plain);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
        assertValueEqual(helper, thirst.exhaustionModifier(player), plain * GameplayConfig.FIRE_RESISTANCE_PERCENT.get() / 100.0F, "multiplier with Fire Resistance");
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        assertValueEqual(helper, thirst.exhaustionModifier(player), plain, "multiplier after Fire Resistance is removed");
        helper.succeed();
    }

    /** The respawn values come with the respawn, not with the death: a death another mod cancels (its own totem) keeps the thirst. */
    @GameTest(template = "empty")
    public static void aCancelledDeathKeepsThirst(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 7, 3, ThirstChangeEvent.Cause.COMMAND);
        player.addTag(IMMORTAL_TAG);
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(player, DamageSource.GENERIC));
        assertValueEqual(helper, thirst.getThirst(), 7, "thirst after a cancelled death");
        assertValueEqual(helper, thirst.getQuenched(), 3, "quenched after a cancelled death");
        helper.succeed();
    }

    /** Respawning after a death gives {@code death.respawnThirst} and {@code respawnQuenched}; coming back from the End is not a death. */
    @GameTest(template = "empty")
    public static void respawningGivesTheRespawnValues(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 7, 3, ThirstChangeEvent.Cause.COMMAND);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, true));
        assertValueEqual(helper, thirst.getThirst(), 7, "thirst after coming back from the End");
        assertValueEqual(helper, thirst.getQuenched(), 3, "quenched after coming back from the End");
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));
        assertValueEqual(helper, thirst.getThirst(), GameplayConfig.RESPAWN_THIRST.get(), "thirst after respawning");
        assertValueEqual(helper, thirst.getQuenched(), GameplayConfig.RESPAWN_QUENCHED.get(), "quenched after respawning");
        assertValueEqual(helper, lastCause, ThirstChangeEvent.Cause.DEATH, "cause of the respawn values");
        helper.succeed();
    }

    /** Dehydration adds thirst exhaustion like any activity: not to creative players nor to players with thirst disabled. */
    @GameTest(template = "empty")
    public static void dehydrationSparesCreativeAndDisabledPlayers(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        MobEffect dehydration = EffectInit.DEHYDRATION.get();
        float start = thirst.getExhaustion();
        dehydration.applyEffectTick(player, 0);
        assertTrue(helper, thirst.getExhaustion() > start, "Dehydration added no exhaustion to a survival player");
        float survival = thirst.getExhaustion();
        player.getAbilities().invulnerable = true;
        dehydration.applyEffectTick(player, 0);
        assertValueEqual(helper, thirst.getExhaustion(), survival, "exhaustion of a creative player with Dehydration");
        player.getAbilities().invulnerable = false;
        thirst.setShouldTickThirst(false);
        dehydration.applyEffectTick(player, 0);
        assertValueEqual(helper, thirst.getExhaustion(), survival, "exhaustion of a player with thirst disabled and Dehydration");
        helper.succeed();
    }

    /** OWN mode: a block break or an attack that another mod cancels (claims, PvP rules) costs no thirst, as it costs no hunger in vanilla. */
    @GameTest(template = "empty")
    public static void cancelledActivitiesCostNothingInOwnMode(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        GameplayConfig.Mode mode = GameplayConfig.MODE.get();
        double basal = GameplayConfig.BASAL_PER_TICK.get();
        try
        {
            set(GameplayConfig.MODE, GameplayConfig.Mode.OWN);
            set(GameplayConfig.BASAL_PER_TICK, 0.0);
            BlockPos pos = helper.absolutePos(BlockPos.ZERO);
            thirst.tick(player);
            float start = thirst.getExhaustion();
            player.addTag(PROTECTED_TAG);
            MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), player));
            MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, player));
            thirst.tick(player);
            assertValueEqual(helper, thirst.getExhaustion(), start, "exhaustion after a cancelled block break and attack");
            player.removeTag(PROTECTED_TAG);
            MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), player));
            thirst.tick(player);
            assertTrue(helper, thirst.getExhaustion() > start, "a block break added no exhaustion in OWN mode");
        }
        finally
        {
            set(GameplayConfig.MODE, mode);
            set(GameplayConfig.BASAL_PER_TICK, basal);
        }
        helper.succeed();
    }

    /** Right clicks reach this mod's handlers on the server too; hand drinking itself only acts on the client. */
    @GameTest(template = "empty")
    public static void rightClicksRunOnTheServer(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickEmpty(player, InteractionHand.MAIN_HAND));
        helper.succeed();
    }

    /** The tooltip line of estimated values: the translated values in gray, kept while they stay the same. */
    @GameTest(template = "empty")
    public static void estimatedLineIsKeptWhileTheValuesStay(GameTestHelper helper)
    {
        Component line = PlayerThirstManager.estimatedLine(3, 2);
        assertValueEqual(helper, line, new TranslatableComponent("blue_droplets.tooltip.estimated", 3, 2).withStyle(ChatFormatting.GRAY), "line for 3 thirst and 2 quenched");
        assertTrue(helper, PlayerThirstManager.estimatedLine(3, 2) == line, "the line is built again for the same values");
        assertValueEqual(helper, PlayerThirstManager.estimatedLine(4, 2), new TranslatableComponent("blue_droplets.tooltip.estimated", 4, 2).withStyle(ChatFormatting.GRAY), "line for 4 thirst and 2 quenched");
        helper.succeed();
    }
}
