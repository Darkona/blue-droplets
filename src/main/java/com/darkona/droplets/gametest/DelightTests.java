package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.foundation.common.event.Events;
import com.darkona.droplets.api.PurityLevel;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.CompatConfig;
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
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.satisfy.herbalbrews.core.blocks.entity.TeaKettleBlockEntity;

import java.util.Arrays;

import static com.darkona.droplets.gametest.TestSupport.player;

import static com.darkona.droplets.gametest.TestSupport.assertValueEqual;

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

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
    public static void farmAndCharmSinkIsHooked(GameTestHelper helper)
    {
        // Farm & Charm has the sink class but other Let's Do mods register the block, so this only checks that the
        // hook applies to it.
        try
        {
            Class<?> sink = Class.forName("net.satisfy.farm_and_charm.core.block.SinkBlock");
            helper.assertTrue(Arrays.stream(sink.getDeclaredMethods()).anyMatch(method -> method.getName().endsWith("blue_droplets$worldPurity")), "the sink hook is not applied");
        }
        catch (ClassNotFoundException e)
        {
            helper.fail("no Farm & Charm sink class");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
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

    @GameTest(template = "empty", templateNamespace = BlueDroplets.ID)
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
}
