package com.darkona.droplets.gametest;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
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
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.darkona.droplets.gametest.TestSupport.player;
import static com.darkona.droplets.gametest.TestSupport.thirst;

/**
 * {@code purity.enabled=false}: nothing stores, shows or rolls a purity. Each test turns it off and back on within
 * the same method, like the other config tests.
 */
public class PurityOffTests
{
    /** Purity of the last drink; tests run one at a time on the server thread. */
    private static int lastDrinkPurity;

    static
    {
        NeoForge.EVENT_BUS.addListener((DrinkEvent.Post event) -> lastDrinkPurity = event.getPurity());
    }

    private static void withPurityOff(Runnable test)
    {
        boolean enabled = PurityConfig.ENABLED.get();
        try
        {
            PurityConfig.ENABLED.set(false);
            test.run();
        }
        finally
        {
            PurityConfig.ENABLED.set(enabled);
        }
    }

    @GameTest(template = "box")
    public static void waterTakenFromTheWorldHasNoPurity(GameTestHelper helper)
    {
        withPurityOff(() -> {
            helper.assertFalse(WaterPurity.hasPurity(WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), 0)), "addPurity stored a purity on an item");
            helper.assertFalse(WaterPurity.hasPurity(WaterPurity.addPurity(new FluidStack(Fluids.WATER, 1000), 0)), "addPurity stored a purity on a fluid");
            helper.assertFalse(WaterPurity.hasPurity(DropletsAPI.withPurity(new ItemStack(Items.WATER_BUCKET), 0)), "the API stored a purity");

            BlockPos water = helper.absolutePos(new BlockPos(2, 2, 2));
            helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
            ServerPlayer player = player(helper);
            player.snapTo(water.getX() + 0.5, water.getY() + 1, water.getZ() + 0.5, 0.0F, 90.0F);
            for (Item empty : new Item[]{Items.GLASS_BOTTLE, ItemInit.TERRACOTTA_BOWL.get(), Items.BUCKET})
            {
                ItemStack stack = new ItemStack(empty);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                ItemStack filled = TestSupport.heldResult(stack.use(player.level(), player, InteractionHand.MAIN_HAND), stack);
                helper.assertTrue(WaterPurity.isWaterFilledContainer(filled), empty + " was not filled from the world");
                helper.assertFalse(WaterPurity.hasPurity(filled), empty + " filled from the world has a purity");
            }

            BlockPos dispenser = helper.absolutePos(new BlockPos(1, 2, 2));
            helper.getLevel().setBlockAndUpdate(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
            helper.getLevel().setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
            BlockSource source = new BlockSource(helper.getLevel(), dispenser, helper.getLevel().getBlockState(dispenser),
                    (DispenserBlockEntity) helper.getLevel().getBlockEntity(dispenser));
            ItemStack dispensed = DispenserBlock.DISPENSER_REGISTRY.get(Items.GLASS_BOTTLE).dispense(source, new ItemStack(Items.GLASS_BOTTLE));
            helper.assertTrue(dispensed.is(Items.POTION), "the dispenser did not fill the bottle");
            helper.assertFalse(WaterPurity.hasPurity(dispensed), "water bottle from a dispenser has a purity");

            helper.assertFalse(WaterPurity.hasPurity(TestSupport.fillBucket(helper, WaterPurity.waterResource(PurityLevel.PURE.level()))), "bucket filled from a tank has a purity");
        });
        helper.succeed();
    }

    @GameTest(template = "box")
    public static void cauldronWaterHasNoPurity(GameTestHelper helper)
    {
        withPurityOff(() -> {
            BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
            helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            for (Item empty : new Item[]{Items.BUCKET, ItemInit.TERRACOTTA_BOWL.get()})
            {
                helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
                ItemStack stack = new ItemStack(empty);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                helper.getLevel().getBlockState(pos).useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                ItemStack filled = player.getItemInHand(InteractionHand.MAIN_HAND);
                helper.assertTrue(WaterPurity.isWaterFilledContainer(filled), empty + " was not filled from the cauldron");
                helper.assertFalse(WaterPurity.hasPurity(filled), empty + " filled from a cauldron has a purity");
            }

            helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            ResourceHandler<FluidResource> handler = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, pos, null);
            FluidResource inTank = handler.getResource(0);
            helper.assertFalse(WaterPurity.hasPurity(inTank), "cauldron tank contents have a purity");
            try (Transaction transaction = Transaction.openRoot())
            {
                helper.assertValueEqual(handler.extract(0, inTank, 1000, transaction), 1000, "water extracted from a cauldron");
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
            ItemStack dirty = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            dirty.set(ThirstComponent.PURITY, 0);
            PlayerThirst.consume(dirty, player);
            helper.assertTrue(player.getActiveEffects().isEmpty(), "dirty water gave effects with purity off: " + player.getActiveEffects());
            helper.assertTrue(thirst.getThirst() > 4, "dirty water did not restore thirst with purity off");
            helper.assertValueEqual(lastDrinkPurity, DropletsAPI.NO_PURITY, "purity of a drink with purity off");

            PlayerThirst.drink(player, ItemStack.EMPTY, 1, 0, 0);
            helper.assertTrue(player.getActiveEffects().isEmpty(), "drinking dirty water by hand gave effects with purity off");
            helper.assertValueEqual(lastDrinkPurity, DropletsAPI.NO_PURITY, "purity of hand drinking with purity off");
            helper.assertTrue(WaterPurity.givePurityEffects(player, 0), "dirty water blocks hydration with purity off");
            helper.assertValueEqual(WaterPurity.waterThirstBonus(PurityLevel.PURE.level()) + WaterPurity.waterQuenchedBonus(PurityLevel.PURE.level()), 0, "pure water bonus with purity off");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestLootHasNoPurity(GameTestHelper helper)
    {
        LootTable table = helper.getLevel().getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, BlueDroplets.asResource("chests/simple_dungeon")));
        LootParams params = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO)).create(LootContextParamSets.CHEST);
        helper.assertTrue(waterBottles(table, params).stream().anyMatch(WaterPurity::hasPurity), "chest loot water bottles have no purity with purity on");
        withPurityOff(() -> {
            List<ItemStack> bottles = waterBottles(table, params);
            helper.assertFalse(bottles.isEmpty(), "no water bottles in 200 rolls of the chest loot");
            helper.assertFalse(bottles.stream().anyMatch(WaterPurity::hasPurity), "chest loot water bottle has a purity with purity off");
        });
        helper.succeed();
    }

    private static List<ItemStack> waterBottles(LootTable table, LootParams params)
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
        helper.assertValueEqual(waterStacks(ThirstTab.DisplayItems()), 3 * PurityLevel.values().length, "water stacks in the tab with purity on (three containers, six purities)");
        withPurityOff(() -> {
            Collection<ItemStack> items = ThirstTab.DisplayItems();
            helper.assertValueEqual(waterStacks(items), 3, "water stacks in the tab with purity off");
            for (ItemStack stack : items)
                helper.assertFalse(WaterPurity.hasPurity(stack), stack + " in the tab has a purity with purity off");
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
        Map<Identifier, Resource> recipes = helper.getLevel().getServer().getResourceManager()
                .listResources("recipe", id -> id.getNamespace().equals(BlueDroplets.ID) && id.getPath().endsWith(".json"));
        helper.assertTrue(recipes.size() > 10, "only " + recipes.size() + " recipes found");
        for (Map.Entry<Identifier, Resource> recipe : recipes.entrySet())
        {
            JsonObject json;
            try (Reader reader = recipe.getValue().openAsReader())
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
            if (json.has("neoforge:conditions"))
                for (JsonElement condition : json.getAsJsonArray("neoforge:conditions"))
                    conditioned |= condition.getAsJsonObject().get("type").getAsString().equals("blue_droplets:purity_enabled");
            helper.assertTrue(conditioned, recipe.getKey() + " uses a purity without the blue_droplets:purity_enabled condition");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purificationPageIsHiddenWithPurityOff(GameTestHelper helper)
    {
        helper.assertFalse(PurificationEntry.all().isEmpty(), "purification page empty with purity on");
        withPurityOff(() -> helper.assertTrue(PurificationEntry.all().isEmpty(), "purification page has entries with purity off"));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugPurityCommandSaysOff(GameTestHelper helper)
    {
        withPurityOff(() -> {
            ServerPlayer player = player(helper);
            CommandSourceStack source = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
            try
            {
                int result = helper.getLevel().getServer().getCommands().getDispatcher().execute("blue_droplets debug purity", source);
                helper.assertValueEqual(result, 0, "result of debug purity with purity off");
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        helper.succeed();
    }
}
