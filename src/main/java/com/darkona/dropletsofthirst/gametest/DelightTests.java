package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.common.event.Events;
import com.darkona.dropletsofthirst.api.PurityLevel;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.satisfy.herbalbrews.core.blocks.entity.TeaKettleBlockEntity;

import java.util.Arrays;

import static com.darkona.dropletsofthirst.gametest.TestSupport.player;

import static com.darkona.dropletsofthirst.gametest.TestSupport.assertValueEqual;

/**
 * Farmer's Delight addons and Let's Do mods ({@code -PwithDelight}): kettles refuse dirty water, the infinite water
 * sources give the world's purity, Extra Delight bottles keep theirs. Registered by {@link DropletsGameTests} only when
 * Extra Delight, Farm & Charm, HerbalBrews and Brewery are all installed.
 */
@PrefixGameTestTemplate(false)
public class DelightTests
{
    private static final int KETTLE_WATER_SLOT = 6;

    private static ItemStack waterBottle(int purity)
    {
        return WaterPurity.addPurity(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), purity);
    }

    private static Block block(String id)
    {
        return BuiltInRegistries.BLOCK.get(new ResourceLocation(id));
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos relative)
    {
        BlockPos pos = helper.absolutePos(relative);
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void farmAndCharmSinkIsHooked(GameTestHelper helper)
    {
        // Farm & Charm has the sink class but other Let's Do mods register the block, so this only checks that the
        // hook applies to it.
        try
        {
            Class<?> sink = Class.forName("net.satisfy.farm_and_charm.core.block.SinkBlock");
            helper.assertTrue(Arrays.stream(sink.getDeclaredMethods()).anyMatch(method -> method.getName().endsWith("droplets_of_thirst$worldPurity")), "the sink hook is not applied");
        }
        catch (ClassNotFoundException e)
        {
            helper.fail("no Farm & Charm sink class");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void teaKettleTakesWaterOfPurityTwoOrMore(GameTestHelper helper)
    {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, block("herbalbrews:tea_kettle"));
        TeaKettleBlockEntity kettle = (TeaKettleBlockEntity) helper.getBlockEntity(pos);
        int min = CompatConfig.KETTLE_MIN_PURITY.get();
        assertValueEqual(helper, min, PurityLevel.MURKY.level(), "default kettleMinPurity");
        for (int purity = WaterPurity.MIN_PURITY; purity < min; purity++)
        {
            kettle.setItem(KETTLE_WATER_SLOT, waterBottle(purity));
            kettle.tick(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos));
            assertValueEqual(helper, kettle.getWaterLevel(), 0, "water level after water of purity " + purity);
            helper.assertTrue(WaterPurity.isWaterFilledContainer(kettle.getItem(KETTLE_WATER_SLOT)), "water of purity " + purity + " left the slot");
        }

        for (int purity = min; purity <= WaterPurity.MAX_PURITY; purity++)
        {
            int before = kettle.getWaterLevel();
            kettle.setItem(KETTLE_WATER_SLOT, waterBottle(purity));
            kettle.tick(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos));
            assertValueEqual(helper, kettle.getWaterLevel(), before + 25, "water level after water of purity " + purity);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void breweryKettleRefusesDirtyWater(GameTestHelper helper)
    {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, block("brewery:wooden_brewingstation"));
        ServerPlayer player = player(helper);
        for (int purity = WaterPurity.MIN_PURITY; purity <= WaterPurity.MAX_PURITY; purity++)
        {
            player.setItemInHand(InteractionHand.MAIN_HAND, WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), purity));
            PlayerInteractEvent.RightClickBlock event = Events.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, helper.absolutePos(pos), hit(helper, pos)));
            assertValueEqual(helper, event.isCanceled(), purity < CompatConfig.KETTLE_MIN_PURITY.get(), "brewing station refuses water of purity " + purity);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.assertFalse(Events.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, helper.absolutePos(pos), hit(helper, pos))).isCanceled(),
                "brewing station refuses water without a purity");
        helper.succeed();
    }

    /**
     * The clean_water_cooking pack: hot recipes of Farm & Charm take water of purity 2 or better, cold ones and
     * Farmer's Delight's cooking pot purity 3 or better; water without a purity counts as acceptable.
     */
    @GameTest(template = "empty", templateNamespace = DropletsOfThirst.ID)
    public static void cookingRecipesNeedCleanWater(GameTestHelper helper)
    {
        waterRule(helper, "farm_and_charm:pot_cooking/nettle_tea", PurityLevel.MURKY.level());
        waterRule(helper, "farm_and_charm:crafting_bowl/dough", PurityLevel.ACCEPTABLE.level());
        if (ModList.get().isLoaded("farmersdelight"))
            waterRule(helper, "farmersdelight:cooking_pot", PurityLevel.ACCEPTABLE.level());
        helper.succeed();
    }

    /**
     * The water ingredient of {@code id} (the one that takes a water bucket) accepts buckets without a purity and of
     * {@code min} or better, and refuses dirtier ones.
     */
    private static void waterRule(GameTestHelper helper, String id, int min)
    {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(new ResourceLocation(id)).orElse(null);
        helper.assertTrue(recipe != null, "no recipe " + id);
        Ingredient water = recipe.getIngredients().stream().filter(ingredient -> ingredient.test(new ItemStack(Items.WATER_BUCKET))).findFirst().orElse(null);
        helper.assertTrue(water != null, id + " takes no water bucket");
        for (int purity = WaterPurity.MIN_PURITY; purity <= WaterPurity.MAX_PURITY; purity++)
            assertValueEqual(helper, water.test(WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), purity)), purity >= min, id + " takes water of purity " + purity);
    }
}
