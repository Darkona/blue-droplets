package com.darkona.droplets.gametest;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.darkona.droplets.content.thirst.PlayerThirstManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.core.registries.Registries;
import com.darkona.droplets.foundation.config.GameplayConfig;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
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
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.UUID;

import static com.darkona.droplets.gametest.TestSupport.player;
import static com.darkona.droplets.gametest.TestSupport.thirst;

/**
 * Drinking, eating, limits, overhydration, purity effects and the commands, with the default config.
 */
public class ThirstTests
{
    /** Cause of the last thirst change of any player; tests run one at a time on the server thread. */
    private static ThirstChangeEvent.Cause lastCause;
    /** Players with this tag do not die: their death is cancelled after every other listener, like a modded totem. */
    private static final String IMMORTAL_TAG = "droplets-test-immortal";
    /** Players with this tag cannot break blocks nor attack: cancelled at low priority, like a claim or PvP mod. */
    private static final String PROTECTED_TAG = "droplets-test-protected";
    /** Thirst changes of players with this tag are cancelled, like a mod that freezes thirst. */
    private static final String FROZEN_TAG = "droplets-test-frozen";

    static
    {
        NeoForge.EVENT_BUS.addListener((ThirstChangeEvent.Post event) -> lastCause = event.getCause());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> {
            if (event.getEntity().entityTags().contains(IMMORTAL_TAG))
                event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, (BreakBlockEvent event) -> {
            if (event.getPlayer().entityTags().contains(PROTECTED_TAG))
                event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, (AttackEntityEvent event) -> {
            if (event.getEntity().entityTags().contains(PROTECTED_TAG))
                event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ThirstChangeEvent.Pre event) -> {
            if (event.getEntity().entityTags().contains(FROZEN_TAG))
                event.setCanceled(true);
        });
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
        PlayerThirst.consume(waterBottle(PurityLevel.PURE.level()), player);
        helper.assertValueEqual(thirst.getThirst(), 10, "thirst after a pure water bottle (4 + 2 pure)");
        helper.assertValueEqual(thirst.getQuenched(), 8, "quenched after a pure water bottle (5 + 3 pure)");
        helper.assertValueEqual(lastCause, ThirstChangeEvent.Cause.DRINK, "cause of drinking a water bottle");
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
        helper.assertValueEqual(thirst.getThirst(), 8, "thirst after a clean water bottle (4)");
        helper.assertValueEqual(thirst.getQuenched(), 5, "quenched after a clean water bottle (5)");

        ThirstValues values = ThirstHelper.drinkValuesOf(waterBottle(pure));
        helper.assertTrue(values != null && values.thirst() == 6 && values.quenched() == 8, "values shown for a pure water bottle: " + values);
        helper.assertTrue(ThirstHelper.drinkValuesOf(waterBottle(pure)) == values, "the pure values are built again for the same stack");
        for (int purity = PurityLevel.MIN; purity < pure; purity++)
        {
            ThirstValues other = ThirstHelper.drinkValuesOf(waterBottle(purity));
            helper.assertTrue(other != null && other.thirst() == 4 && other.quenched() == 5, "values shown for a water bottle of purity " + purity + ": " + other);
        }

        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        PlayerThirst.drinkWater(player, 3, 2, pure);
        helper.assertValueEqual(thirst.getThirst(), 9, "thirst after a hose sip of pure water (3 + 2)");
        helper.assertValueEqual(thirst.getQuenched(), 5, "quenched after a hose sip of pure water (2 + 3)");
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

    /** Minecraft 26.1 eats through the food component of the consumed stack: the hydration hook is there now. */
    @GameTest(template = "empty")
    public static void eatingAnAppleHydratesOnce(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 4, 0, ThirstChangeEvent.Cause.COMMAND);
        ItemStack apples = new ItemStack(Items.APPLE, 2);
        apples.finishUsingItem(helper.getLevel(), player);
        helper.assertValueEqual(apples.getCount(), 1, "apples left after eating one");
        helper.assertValueEqual(thirst.getThirst(), 6, "thirst after eating an apple (2)");
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
        helper.assertFalse(player.hasEffect(MobEffects.NAUSEA), "food rolled purity effects");
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
        helper.assertTrue(player.hasEffect(MobEffects.NAUSEA), "Overhydrated comes with Nausea");
        helper.assertTrue(player.getEffect(MobEffects.NAUSEA).getDuration() >= 200, "Overhydrated brings at least 10 seconds of Nausea");
        helper.succeed();
    }

    /** Water that another mod keeps from being drunk ({@code ThirstChangeEvent.Pre} cancelled) is not drunk past full either. */
    @GameTest(template = "empty")
    public static void cancelledDrinksDoNotOverhydrate(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 20, 12, ThirstChangeEvent.Cause.COMMAND);
        player.addTag(FROZEN_TAG);
        for (int i = 0; i < 4; i++)
            thirst.hydrate(player, 6, 8, true, ThirstChangeEvent.Cause.DRINK);
        player.removeTag(FROZEN_TAG);
        helper.assertFalse(player.hasEffect(EffectInit.OVERHYDRATED), "cancelled drinks overhydrated");
        thirst.hydrate(player, 6, 8, true, ThirstChangeEvent.Cause.DRINK);
        helper.assertFalse(player.hasEffect(EffectInit.OVERHYDRATED), "one bottle past full after cancelled drinks overhydrated");
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
        MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
        helper.assertTrue(nausea != null, "dirty water gives Nausea (100% by default)");
        // Vanilla only builds up the screen distortion while more than 60 ticks are left, at 1/150 per tick.
        helper.assertTrue(nausea.getDuration() >= 200, "dirty water gives at least 10 seconds of Nausea, not " + nausea.getDuration() + " ticks");
        helper.assertTrue(player.hasEffect(MobEffects.HUNGER), "dirty water gives Hunger (100% by default)");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cleanAndPureWaterGiveNoEffects(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst.consume(waterBottle(PurityLevel.CLEAN.level()), player);
        helper.assertTrue(player.getActiveEffects().isEmpty(), "clean water gave " + player.getActiveEffects());
        PlayerThirst.consume(waterBottle(PurityLevel.PURE.level()), player);
        helper.assertTrue(player.getActiveEffects().isEmpty(), "pure water gave " + player.getActiveEffects());
        helper.succeed();
    }

    /** Default {@code [effects]}: contaminated and dirty water can poison, murky and acceptable only sicken, clean and pure do nothing. */
    @GameTest(template = "empty")
    public static void defaultEffectTablesFollowTheSixLevels(GameTestHelper helper)
    {
        double[][] chances = {{100, 100, 40}, {60, 60, 15}, {25, 25}, {5, 5}, {}, {}};
        helper.assertValueEqual(PurityConfig.EFFECTS.size(), PurityLevel.values().length, "effect lists");
        for (PurityLevel level : PurityLevel.values())
        {
            WaterPurity.PurityEffect[] table = WaterPurity.effectTable(level.level());
            double[] expected = chances[level.level()];
            helper.assertValueEqual(table.length, expected.length, "effects of " + level.id());
            for (int i = 0; i < table.length; i++)
            {
                helper.assertTrue(Math.abs(table[i].chance() * 100 - expected[i]) < 0.001, level.id() + " effect " + i + " chance " + table[i].chance());
                helper.assertValueEqual(table[i].blocksHydration(), i == 2, level.id() + " effect " + i + " blocks hydration");
            }
        }
        helper.assertValueEqual(PurityConfig.HOT_DIRTY_WATER_MAX_PURITY.getDefault(), PurityLevel.MURKY.level(), "hotDirtyWater.maxPurity default");
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
        var plains = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        float atSea = ThirstHelper.biomeClimate(level, plains, low, 1.0F, false);
        float onTop = ThirstHelper.biomeClimate(level, plains, high, 1.0F, false);
        helper.assertTrue(onTop < atSea, "climate multiplier 200 blocks above sea level " + onTop + ", not below " + atSea);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theEndIsCold(GameTestHelper helper)
    {
        ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "no End");
        ServerPlayer player = FakePlayerFactory.get(end, new GameProfile(UUID.randomUUID(), "droplets-test"));
        player.snapTo(0.5, 64, 0.5);
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
        CommandSourceStack source = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        helper.assertValueEqual(dispatcher.execute("blue_droplets set @s 4 10", source), 4, "result of set");
        helper.assertValueEqual(thirst(player).getQuenched(), 4, "quenched after set 4 10");
        helper.assertValueEqual(dispatcher.execute("blue_droplets query @s", source), 4, "result of query");
        helper.assertValueEqual(dispatcher.execute("thirst query @s", source), 4, "result of the /thirst alias");
        helper.assertValueEqual(dispatcher.execute("blue_droplets enable @s false", source), 1, "result of enable");
        helper.assertFalse(thirst(player).getShouldTickThirst(), "thirst still enabled after enable false");
        helper.succeed();
    }

    /** Gaining or losing an effect recomputes the thirst loss multiplier right away (Fire Resistance: none by default). */
    @GameTest(template = "empty")
    public static void effectsRecomputeTheMultiplier(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        float plain = thirst.exhaustionModifier(player);
        helper.assertTrue(plain > 0.0F, "multiplier without effects " + plain);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
        helper.assertValueEqual(thirst.exhaustionModifier(player), plain * GameplayConfig.FIRE_RESISTANCE_PERCENT.get() / 100.0F, "multiplier with Fire Resistance");
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        helper.assertValueEqual(thirst.exhaustionModifier(player), plain, "multiplier after Fire Resistance is removed");
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
        NeoForge.EVENT_BUS.post(new LivingDeathEvent(player, player.damageSources().generic()));
        helper.assertValueEqual(thirst.getThirst(), 7, "thirst after a cancelled death");
        helper.assertValueEqual(thirst.getQuenched(), 3, "quenched after a cancelled death");
        helper.succeed();
    }

    /** Respawning after a death gives {@code death.respawnThirst} and {@code respawnQuenched}; coming back from the End is not a death. */
    @GameTest(template = "empty")
    public static void respawningGivesTheRespawnValues(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 7, 3, ThirstChangeEvent.Cause.COMMAND);
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, true));
        helper.assertValueEqual(thirst.getThirst(), 7, "thirst after coming back from the End");
        helper.assertValueEqual(thirst.getQuenched(), 3, "quenched after coming back from the End");
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));
        helper.assertValueEqual(thirst.getThirst(), GameplayConfig.RESPAWN_THIRST.get(), "thirst after respawning");
        helper.assertValueEqual(thirst.getQuenched(), GameplayConfig.RESPAWN_QUENCHED.get(), "quenched after respawning");
        helper.assertValueEqual(lastCause, ThirstChangeEvent.Cause.DEATH, "cause of the respawn values");
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
        dehydration.applyEffectTick(helper.getLevel(), player, 0);
        helper.assertTrue(thirst.getExhaustion() > start, "Dehydration added no exhaustion to a survival player");
        float survival = thirst.getExhaustion();
        player.getAbilities().invulnerable = true;
        dehydration.applyEffectTick(helper.getLevel(), player, 0);
        helper.assertValueEqual(thirst.getExhaustion(), survival, "exhaustion of a creative player with Dehydration");
        player.getAbilities().invulnerable = false;
        thirst.setShouldTickThirst(false);
        dehydration.applyEffectTick(helper.getLevel(), player, 0);
        helper.assertValueEqual(thirst.getExhaustion(), survival, "exhaustion of a player with thirst disabled and Dehydration");
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
            GameplayConfig.MODE.set(GameplayConfig.Mode.OWN);
            GameplayConfig.BASAL_PER_TICK.set(0.0);
            BlockPos pos = helper.absolutePos(BlockPos.ZERO);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, BlockPos.ZERO.above());
            thirst.tick(player);
            float start = thirst.getExhaustion();
            player.addTag(PROTECTED_TAG);
            NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), player));
            player.attack(zombie);
            thirst.tick(player);
            helper.assertValueEqual(thirst.getExhaustion(), start, "exhaustion after a cancelled block break and attack");
            player.removeTag(PROTECTED_TAG);
            NeoForge.EVENT_BUS.post(new BreakBlockEvent(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), player));
            thirst.tick(player);
            helper.assertTrue(thirst.getExhaustion() > start, "a block break added no exhaustion in OWN mode");
        }
        finally
        {
            GameplayConfig.MODE.set(mode);
            GameplayConfig.BASAL_PER_TICK.set(basal);
        }
        helper.succeed();
    }

    /** OWN mode: an attack costs thirst only when it lands, as vanilla charges hunger; a swing at an invulnerable mob costs nothing. */
    @GameTest(template = "empty")
    public static void onlyLandedAttacksCostThirstInOwnMode(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        GameplayConfig.Mode mode = GameplayConfig.MODE.get();
        double basal = GameplayConfig.BASAL_PER_TICK.get();
        try
        {
            GameplayConfig.MODE.set(GameplayConfig.Mode.OWN);
            GameplayConfig.BASAL_PER_TICK.set(0.0);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, BlockPos.ZERO.above());
            zombie.setInvulnerable(true);
            thirst.tick(player);
            float start = thirst.getExhaustion();
            player.attack(zombie);
            thirst.tick(player);
            helper.assertValueEqual(thirst.getExhaustion(), start, "exhaustion after an attack on an invulnerable mob");
            zombie.setInvulnerable(false);
            player.attack(zombie);
            thirst.tick(player);
            helper.assertTrue(thirst.getExhaustion() > start, "an attack that landed added no exhaustion in OWN mode");
        }
        finally
        {
            GameplayConfig.MODE.set(mode);
            GameplayConfig.BASAL_PER_TICK.set(basal);
        }
        helper.succeed();
    }

    /** Right clicks reach this mod's handlers on the server too; hand drinking itself only acts on the client. */
    @GameTest(template = "empty")
    public static void rightClicksRunOnTheServer(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickEmpty(player, InteractionHand.MAIN_HAND));
        helper.succeed();
    }

    /** The tooltip line of estimated values: the translated values in gray, kept while they stay the same. */
    @GameTest(template = "empty")
    public static void estimatedLineIsKeptWhileTheValuesStay(GameTestHelper helper)
    {
        Component line = PlayerThirstManager.estimatedLine(3, 2);
        helper.assertValueEqual(line, Component.translatable("blue_droplets.tooltip.estimated", 3, 2).withStyle(ChatFormatting.GRAY), "line for 3 thirst and 2 quenched");
        helper.assertTrue(PlayerThirstManager.estimatedLine(3, 2) == line, "the line is built again for the same values");
        helper.assertValueEqual(PlayerThirstManager.estimatedLine(4, 2), Component.translatable("blue_droplets.tooltip.estimated", 4, 2).withStyle(ChatFormatting.GRAY), "line for 4 thirst and 2 quenched");
        helper.succeed();
    }
}
