package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.DropletsAPI;
import com.darkona.droplets.api.event.DrinkEvent;
import com.darkona.droplets.compat.jei.PurificationEntry;
import com.darkona.droplets.api.PurityLevel;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ItemInit;
import com.darkona.droplets.content.registry.ThirstComponent;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.foundation.config.PurityConfig;
import com.darkona.droplets.foundation.tab.ThirstTab;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.darkona.droplets.gametest.TestSupport.player;
import static com.darkona.droplets.gametest.TestSupport.thirst;

import static com.darkona.droplets.gametest.TestSupport.assertValueEqual;
import static com.darkona.droplets.gametest.TestSupport.assertTrue;
import static com.darkona.droplets.gametest.TestSupport.assertFalse;

/**
 * {@code purity.enabled=false}: nothing stores, shows or rolls a purity. Each test turns it off and back on within
 * the same method, like the other config tests.
 */
@GameTestHolder(BlueDroplets.ID)
@PrefixGameTestTemplate(false)
public class PurityOffTests
{
    /** Purity of the last drink; tests run one at a time on the server thread. */
    private static int lastDrinkPurity;

    static
    {
        MinecraftForge.EVENT_BUS.addListener((DrinkEvent.Post event) -> lastDrinkPurity = event.getPurity());
    }

    private static void withPurityOff(Runnable test)
    {
        boolean enabled = PurityConfig.ENABLED.get();
        try
        {
            TestSupport.set(PurityConfig.ENABLED, false);
            test.run();
        }
        finally
        {
            TestSupport.set(PurityConfig.ENABLED, enabled);
        }
    }

    @GameTest(template = "box")
    public static void waterTakenFromTheWorldHasNoPurity(GameTestHelper helper)
    {
        withPurityOff(() -> {
            assertFalse(helper, WaterPurity.hasPurity(WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), 0)), "addPurity stored a purity on an item");
            assertFalse(helper, WaterPurity.hasPurity(WaterPurity.addPurity(new FluidStack(Fluids.WATER, 1000), 0)), "addPurity stored a purity on a fluid");
            assertFalse(helper, WaterPurity.hasPurity(DropletsAPI.withPurity(new ItemStack(Items.WATER_BUCKET), 0)), "the API stored a purity");

            BlockPos water = helper.absolutePos(new BlockPos(2, 2, 2));
            helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
            ServerPlayer player = player(helper);
            player.moveTo(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 0.0F, 90.0F);
            for (Item empty : new Item[]{Items.GLASS_BOTTLE, ItemInit.TERRACOTTA_BOWL.get(), Items.BUCKET})
            {
                ItemStack stack = new ItemStack(empty);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                ItemStack filled = stack.use(player.level, player, InteractionHand.MAIN_HAND).getObject();
                assertTrue(helper, WaterPurity.isWaterFilledContainer(filled), empty + " was not filled from the world");
                assertFalse(helper, WaterPurity.hasPurity(filled), empty + " filled from the world has a purity");
            }

            BlockPos dispenser = helper.absolutePos(new BlockPos(1, 2, 2));
            helper.getLevel().setBlockAndUpdate(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
            helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
            DispenserBlockEntity entity = (DispenserBlockEntity) helper.getLevel().getBlockEntity(dispenser);
            entity.setItem(0, new ItemStack(Items.GLASS_BOTTLE));
            helper.getLevel().getBlockState(dispenser).tick(helper.getLevel(), dispenser, helper.getLevel().random);
            ItemStack dispensed = entity.getItem(0);
            assertTrue(helper, dispensed.is(Items.POTION), "the dispenser did not fill the bottle");
            assertFalse(helper, WaterPurity.hasPurity(dispensed), "water bottle from a dispenser has a purity");

            assertFalse(helper, WaterPurity.hasPurity(FluidUtil.getFilledBucket(new FluidStack(Fluids.WATER, 1000))), "bucket filled from a tank has a purity");
        });
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void cauldronWaterHasNoPurity(GameTestHelper helper)
    {
        withPurityOff(() -> {
            BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
            helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
            Player player = TestSupport.player(helper);
            for (Item empty : new Item[]{Items.BUCKET, ItemInit.TERRACOTTA_BOWL.get()})
            {
                helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
                ItemStack stack = new ItemStack(empty);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                helper.getLevel().getBlockState(pos).use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                ItemStack filled = player.getItemInHand(InteractionHand.MAIN_HAND);
                assertTrue(helper, WaterPurity.isWaterFilledContainer(filled), empty + " was not filled from the cauldron");
                assertFalse(helper, WaterPurity.hasPurity(filled), empty + " filled from a cauldron has a purity");
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkingGivesNoPurityEffects(GameTestHelper helper)
    {
        withPurityOff(() -> {
            ServerPlayer player = player(helper);
            PlayerThirst thirst = thirst(player);
            thirst.setThirst(4);
            ItemStack dirty = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
            dirty.getOrCreateTag().putInt(ThirstComponent.PURITY, 0);
            PlayerThirst.consume(dirty, player);
            assertTrue(helper, player.getActiveEffects().isEmpty(), "dirty water gave effects with purity off: " + player.getActiveEffects());
            assertTrue(helper, thirst.getThirst() > 4, "dirty water did not restore thirst with purity off");
            assertValueEqual(helper, lastDrinkPurity, DropletsAPI.NO_PURITY, "purity of a drink with purity off");

            PlayerThirst.drink(player, ItemStack.EMPTY, 1, 0, 0);
            assertTrue(helper, player.getActiveEffects().isEmpty(), "drinking dirty water by hand gave effects with purity off");
            assertValueEqual(helper, lastDrinkPurity, DropletsAPI.NO_PURITY, "purity of hand drinking with purity off");
            assertTrue(helper, WaterPurity.givePurityEffects(player, 0), "dirty water blocks hydration with purity off");
            assertValueEqual(helper, WaterPurity.waterThirstBonus(PurityLevel.PURE.level()) + WaterPurity.waterQuenchedBonus(PurityLevel.PURE.level()), 0, "pure water bonus with purity off");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestLootHasNoPurity(GameTestHelper helper)
    {
        LootTable table = helper.getLevel().getServer().getLootTables().get(BlueDroplets.asResource("chests/simple_dungeon"));
        LootContext params = new LootContext.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO)).create(LootContextParamSets.CHEST);
        assertTrue(helper, waterBottles(table, params).stream().anyMatch(WaterPurity::hasPurity), "chest loot water bottles have no purity with purity on");
        withPurityOff(() -> {
            List<ItemStack> bottles = waterBottles(table, params);
            assertFalse(helper, bottles.isEmpty(), "no water bottles in 200 rolls of the chest loot");
            assertFalse(helper, bottles.stream().anyMatch(WaterPurity::hasPurity), "chest loot water bottle has a purity with purity off");
        });
        helper.succeed();
    }

    private static List<ItemStack> waterBottles(LootTable table, LootContext params)
    {
        List<ItemStack> bottles = new ArrayList<>();
        for (int roll = 0; roll < 200; roll++)
            for (ItemStack stack : table.getRandomItems(params))
                if (stack.is(Items.POTION))
                    bottles.add(stack);
        return bottles;
    }

    @GameTest(template = "empty")
    public static void creativeTabHasOneOfEachWater(GameTestHelper helper)
    {
        assertValueEqual(helper, waterStacks(ThirstTab.DisplayItems()), 3 * PurityLevel.values().length, "water stacks in the tab with purity on (three containers, six purities)");
        withPurityOff(() -> {
            Collection<ItemStack> items = ThirstTab.DisplayItems();
            assertValueEqual(helper, waterStacks(items), 3, "water stacks in the tab with purity off");
            for (ItemStack stack : items)
                assertFalse(helper, WaterPurity.hasPurity(stack), stack + " in the tab has a purity with purity off");
        });
        helper.succeed();
    }

    private static int waterStacks(Collection<ItemStack> items)
    {
        int count = 0;
        for (ItemStack stack : items)
            if (WaterPurity.isWaterFilledContainer(stack))
                count++;
        return count;
    }

    /**
     * Recipes are only checked when datapacks load, so this looks at the files: every recipe of this mod that stores or
     * matches a purity must carry the {@code blue_droplets:purity_enabled} condition, or it would load with purity off.
     */
    @GameTest(template = "empty")
    public static void purityRecipesCarryThePurityCondition(GameTestHelper helper)
    {
        ResourceManager manager = helper.getLevel().getServer().getResourceManager();
        List<ResourceLocation> recipes = manager.listResources("recipes", path -> path.endsWith(".json")).stream()
                .filter(id -> id.getNamespace().equals(BlueDroplets.ID)).toList();
        assertTrue(helper, recipes.size() > 10, "only " + recipes.size() + " recipes found");
        for (ResourceLocation recipe : recipes)
        {
            JsonObject json;
            try (Resource resource = manager.getResource(recipe); Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
            {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
            if (!json.toString().contains("\"blue_droplets:purity\":"))
                continue;
            boolean conditioned = false;
            if (json.has("conditions"))
                for (JsonElement condition : json.getAsJsonArray("conditions"))
                    conditioned |= condition.getAsJsonObject().get("type").getAsString().equals("blue_droplets:purity_enabled");
            assertTrue(helper, conditioned, recipe + " uses a purity without the blue_droplets:purity_enabled condition");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purificationPageIsHiddenWithPurityOff(GameTestHelper helper)
    {
        assertFalse(helper, PurificationEntry.all().isEmpty(), "purification page empty with purity on");
        withPurityOff(() -> assertTrue(helper, PurificationEntry.all().isEmpty(), "purification page has entries with purity off"));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugPurityCommandSaysOff(GameTestHelper helper)
    {
        withPurityOff(() -> {
            ServerPlayer player = player(helper);
            CommandSourceStack source = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
            try
            {
                int result = helper.getLevel().getServer().getCommands().getDispatcher().execute("blue_droplets debug purity", source);
                assertValueEqual(helper, result, 0, "result of debug purity with purity off");
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        helper.succeed();
    }
}
