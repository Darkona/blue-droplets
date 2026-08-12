package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.EffectInit;
import com.darkona.droplets.foundation.config.GameplayConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * Data that Minecraft 26.3 reads with formats of its own and ignores in silence when they are wrong: the brewing
 * recipes of the Potion of Quenchness ({@code minecraft:brewing}) and the chest loot (functions and conditions).
 */
public class DataTests
{
    @GameTest(template = "box")
    public static void quenchnessBrewsInABrewingStand(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState());
        if (!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand))
            throw helper.assertionException("no brewing stand block entity");
        stand.setItem(0, PotionContents.createItemStack(Items.POTION, Potions.AWKWARD));
        stand.setItem(3, new ItemStack(Items.PRISMARINE_CRYSTALS));
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));
        for (int tick = 0; tick < 1000 && !stand.getItem(3).isEmpty(); tick++)
            BrewingStandBlockEntity.serverTick(level, pos, level.getBlockState(pos), stand);
        assertPotion(helper, stand.getItem(0), Items.POTION, EffectInit.QUENCHNESS_POTION, "brewing stand result");
        helper.succeed();
    }

    /** One mix and one container change per container; the rest of the 15 recipes come from the same generator. */
    @GameTest(template = "empty")
    public static void quenchnessRecipesCoverEveryContainer(GameTestHelper helper)
    {
        assertBrews(helper, Items.POTION, EffectInit.QUENCHNESS_POTION, Items.REDSTONE, Items.POTION, EffectInit.LONG_QUENCHNESS_POTION);
        assertBrews(helper, Items.SPLASH_POTION, EffectInit.QUENCHNESS_POTION, Items.GLOWSTONE_DUST, Items.SPLASH_POTION, EffectInit.STRONG_QUENCHNESS_POTION);
        assertBrews(helper, Items.LINGERING_POTION, Potions.AWKWARD, Items.PRISMARINE_CRYSTALS, Items.LINGERING_POTION, EffectInit.QUENCHNESS_POTION);
        assertBrews(helper, Items.POTION, EffectInit.STRONG_QUENCHNESS_POTION, Items.GUNPOWDER, Items.SPLASH_POTION, EffectInit.STRONG_QUENCHNESS_POTION);
        assertBrews(helper, Items.SPLASH_POTION, EffectInit.LONG_QUENCHNESS_POTION, Items.DRAGON_BREATH, Items.LINGERING_POTION, EffectInit.LONG_QUENCHNESS_POTION);
        helper.succeed();
    }

    /**
     * {@code effects.quenchnessPotion} acts through a load condition, checked when datapacks load; this decodes the
     * conditions of every brewing recipe of the mod and tests them with the option on and off.
     */
    @GameTest(template = "empty")
    public static void quenchnessRecipesFollowTheConfig(GameTestHelper helper)
    {
        Map<Identifier, Resource> recipes = helper.getLevel().getServer().getResourceManager()
                .listResources("recipe/brewing", id -> id.getNamespace().equals(BlueDroplets.ID) && id.getPath().endsWith(".json"));
        helper.assertValueEqual(recipes.size(), 15, "brewing recipes of the mod");
        boolean enabled = GameplayConfig.QUENCHNESS_POTION.get();
        try
        {
            for (Map.Entry<Identifier, Resource> recipe : recipes.entrySet())
            {
                List<ICondition> conditions = conditions(recipe.getValue());
                helper.assertFalse(conditions.isEmpty(), recipe.getKey() + " has no load condition");
                GameplayConfig.QUENCHNESS_POTION.set(true);
                helper.assertTrue(conditions.stream().allMatch(c -> c.test(ICondition.IContext.EMPTY)), recipe.getKey() + " does not load with quenchnessPotion on");
                GameplayConfig.QUENCHNESS_POTION.set(false);
                helper.assertFalse(conditions.stream().allMatch(c -> c.test(ICondition.IContext.EMPTY)), recipe.getKey() + " loads with quenchnessPotion off");
            }
        }
        finally
        {
            GameplayConfig.QUENCHNESS_POTION.set(enabled);
        }
        helper.succeed();
    }

    /**
     * The chest tables in the 26.3 loot format: set_potion, set_count and the purity (set_components with its loot
     * condition) apply, and the global loot modifiers add them only to the chest named in their condition.
     */
    @GameTest(template = "empty")
    public static void chestLootAppliesItsFunctionsOnlyToItsChest(GameTestHelper helper)
    {
        LootParams params = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO)).create(LootContextParamSets.CHEST);
        int bottles = 0, stacked = 0;
        for (int roll = 0; roll < 200; roll++)
            for (ItemStack stack : lootTable(helper, Identifier.withDefaultNamespace("chests/simple_dungeon")).getRandomItems(params))
                if (WaterPurity.hasPurity(stack))
                {
                    bottles++;
                    assertPotion(helper, stack, Items.POTION, Potions.WATER, "chest loot bottle");
                    helper.assertTrue(stack.getCount() >= 1 && stack.getCount() <= 3, "chest loot count " + stack.getCount() + " outside 1-3");
                    if (stack.getCount() > 1)
                        stacked++;
                }
        helper.assertTrue(bottles > 0, "no water bottles with a purity in 200 rolls of minecraft:chests/simple_dungeon");
        helper.assertTrue(stacked > 0, "set_count never gave more than one bottle");
        for (int roll = 0; roll < 200; roll++)
            for (ItemStack stack : lootTable(helper, Identifier.withDefaultNamespace("chests/igloo_chest")).getRandomItems(params))
                helper.assertFalse(WaterPurity.hasPurity(stack), "minecraft:chests/igloo_chest got " + stack + " from a Blue Droplets loot modifier");
        helper.succeed();
    }

    private static LootTable lootTable(GameTestHelper helper, Identifier id)
    {
        return helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
    }

    private static List<ICondition> conditions(Resource resource)
    {
        JsonElement json;
        try (Reader reader = resource.openAsReader())
        {
            json = JsonParser.parseReader(reader).getAsJsonObject().get("neoforge:conditions");
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        return json == null ? List.of() : ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private static void assertBrews(GameTestHelper helper, Item container, Holder<Potion> potion, Item reagent, Item resultContainer, Holder<Potion> result)
    {
        BrewingInput input = new BrewingInput(PotionContents.createItemStack(container, potion), new ItemStack(reagent));
        ItemStack brewed = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.BREWING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input))
                .orElseThrow(() -> helper.assertionException("no brewing recipe for " + input.input() + " + " + reagent));
        assertPotion(helper, brewed, resultContainer, result, "brewing " + input.input() + " + " + reagent);
    }

    private static void assertPotion(GameTestHelper helper, ItemStack stack, Item container, Holder<Potion> potion, String what)
    {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        helper.assertTrue(stack.is(container) && contents != null && contents.is(potion), what + ": " + stack + " with " + contents);
    }
}
