package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.api.PurityLevel;
import com.darkona.dropletsofthirst.api.event.ThirstChangeEvent;
import com.darkona.dropletsofthirst.content.data.DrinkValues;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.content.registry.ThirstComponent;
import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.darkona.dropletsofthirst.foundation.network.message.PlayerThirstSyncMessage;
import com.darkona.dropletsofthirst.foundation.network.message.ThirstValuesSyncMessage;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.darkona.dropletsofthirst.gametest.TestSupport.player;
import static com.darkona.dropletsofthirst.gametest.TestSupport.thirst;

import static com.darkona.dropletsofthirst.gametest.TestSupport.assertValueEqual;

/**
 * Saved data and codecs: the player capability, the purity on disk and on the wire, the drinks data map and
 * the payloads the HUD is fed from.
 */
@GameTestHolder(DropletsOfThirst.ID)
@PrefixGameTestTemplate(false)
public class SaveTests
{
    @GameTest(template = "empty")
    public static void playerThirstSurvivesASave(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        PlayerThirst thirst = thirst(player);
        thirst.change(player, 13, 7, ThirstChangeEvent.Cause.COMMAND);
        thirst.setExhaustion(2.5F);
        thirst.setShouldTickThirst(false);
        CompoundTag tag = thirst.serializeNBT();
        PlayerThirst loaded = new PlayerThirst();
        loaded.deserializeNBT(tag);
        assertValueEqual(helper, loaded.getThirst(), 13, "thirst after a save");
        assertValueEqual(helper, loaded.getQuenched(), 7, "quenched after a save");
        assertValueEqual(helper, loaded.getExhaustion(), 2.5F, "exhaustion after a save");
        helper.assertFalse(loaded.getShouldTickThirst(), "disabled thirst came back enabled");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void oldSavesLoadWithSaneValues(GameTestHelper helper)
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt("thirst", 10);
        tag.putInt("quenched", 15);
        PlayerThirst loaded = new PlayerThirst();
        loaded.deserializeNBT(tag);
        assertValueEqual(helper, loaded.getQuenched(), 10, "quenched above thirst in an old save");
        helper.assertTrue(loaded.getShouldTickThirst(), "a save without the enable flag is disabled");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purityComponentSurvivesASave(GameTestHelper helper)
    {
        ItemStack bucket = WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), 1);
        ItemStack loaded = ItemStack.of(bucket.save(new CompoundTag()));
        assertValueEqual(helper, ThirstComponent.get(loaded), 1, "purity after a save");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidPurityLoadsAsDefault(GameTestHelper helper)
    {
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        bucket.getOrCreateTag().putInt(ThirstComponent.PURITY, 7);
        ItemStack loaded = ItemStack.of(bucket.save(new CompoundTag()));
        helper.assertTrue(loaded.is(Items.WATER_BUCKET), "a bucket with purity 7 was lost on load");
        assertValueEqual(helper, ThirstComponent.get(loaded), WaterPurity.defaultPurity(), "purity 7 after a load");
        helper.succeed();
    }

    /** Thirst Was Taken's {@code Purity} had four levels: 0, 1, 2 and 3 load as 0, 1, 3 and 5, on water only. */
    @GameTest(template = "empty")
    public static void thirstWasTakenPurityMovesToSixLevels(GameTestHelper helper)
    {
        int[] expected = {PurityLevel.CONTAMINATED.level(), PurityLevel.DIRTY.level(), PurityLevel.ACCEPTABLE.level(), PurityLevel.PURE.level()};
        for (int old = 0; old < expected.length; old++)
        {
            ItemStack bucket = ItemStack.of(legacyItem("minecraft:water_bucket", old));
            assertValueEqual(helper, ThirstComponent.get(bucket), expected[old], "item purity from Thirst Was Taken " + old);
            helper.assertFalse(bucket.getTag().contains(ThirstComponent.LEGACY_PURITY), "the old purity stayed on the item");

            CompoundTag fluid = new CompoundTag();
            fluid.putString("FluidName", "minecraft:water");
            fluid.putInt("Amount", 1000);
            CompoundTag fluidTag = new CompoundTag();
            fluidTag.putInt(ThirstComponent.LEGACY_PURITY, old);
            fluid.put("Tag", fluidTag);
            FluidStack water = FluidStack.loadFluidStackFromNBT(fluid);
            assertValueEqual(helper, ThirstComponent.get(water), expected[old], "fluid purity from Thirst Was Taken " + old);
            helper.assertFalse(water.getTag().contains(ThirstComponent.LEGACY_PURITY), "the old purity stayed on the fluid");
        }
        ItemStack stone = ItemStack.of(legacyItem("minecraft:stone", 2));
        assertValueEqual(helper, stone.getTag().getInt(ThirstComponent.LEGACY_PURITY), 2, "Purity of an item that is no water container");
        helper.assertFalse(ThirstComponent.has(stone), "an item that is no water container got a purity");
        helper.succeed();
    }

    private static CompoundTag legacyItem(String id, int purity)
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(ThirstComponent.LEGACY_PURITY, purity);
        CompoundTag item = new CompoundTag();
        item.putString("id", id);
        item.putByte("Count", (byte) 1);
        item.put("tag", tag);
        return item;
    }

    /** Thirst Was Taken saved the player's thirst as the capability {@code thirst:thirst}, with the same keys. */
    @GameTest(template = "empty")
    public static void thirstWasTakenPlayerDataLoads(GameTestHelper helper)
    {
        ServerPlayer player = player(helper);
        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        CompoundTag caps = saved.getCompound("ForgeCaps");
        caps.remove(ModAttachment.PLAYER_THIRST_ID.toString());
        CompoundTag old = new CompoundTag();
        old.putInt("thirst", 9);
        old.putInt("quenched", 3);
        old.putFloat("exhaustion", 1.5F);
        old.putBoolean("enable", true);
        caps.put("thirst:thirst", old);
        ServerPlayer loaded = player(helper);
        loaded.load(saved);
        assertValueEqual(helper, thirst(loaded).getThirst(), 9, "thirst from Thirst Was Taken");
        assertValueEqual(helper, thirst(loaded).getQuenched(), 3, "quenched from Thirst Was Taken");

        thirst(loaded).change(loaded, 15, 5, ThirstChangeEvent.Cause.COMMAND);
        CompoundTag again = loaded.saveWithoutId(new CompoundTag());
        ServerPlayer reloaded = player(helper);
        reloaded.load(again);
        assertValueEqual(helper, thirst(reloaded).getThirst(), 15, "thirst after saving under the new id");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purityComponentCrossesTheNetwork(GameTestHelper helper)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeItem(WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), 1));
        ItemStack received = buf.readItem();
        assertValueEqual(helper, ThirstComponent.get(received), 1, "purity after the network");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinksDataMapAcceptsSaltyAndRejectsOutOfRange(GameTestHelper helper)
    {
        helper.assertTrue(parse("{\"thirst\": -2, \"quenched\": -2}"), "salty entry rejected");
        helper.assertTrue(parse("{\"thirst\": 6, \"quenched\": 8, \"purity\": 5}"), "water entry of purity 5 rejected");
        helper.assertFalse(parse("{\"thirst\": 21, \"quenched\": 0}"), "thirst 21 accepted");
        helper.assertFalse(parse("{\"thirst\": 2, \"quenched\": -30}"), "quenched -30 accepted");
        helper.assertFalse(parse("{\"thirst\": 2, \"quenched\": 2, \"purity\": 6}"), "purity 6 accepted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void thirstSyncPayloadRoundTrips(GameTestHelper helper)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        PlayerThirstSyncMessage sent = new PlayerThirstSyncMessage(17, 9, 1.25F, PlayerThirst.SYNC_ENABLED | PlayerThirst.SYNC_SPRINT_BLOCKED | 6 << PlayerThirst.SYNC_SPRINT_MIN_SHIFT);
        sent.encode(buf);
        assertValueEqual(helper, PlayerThirstSyncMessage.decode(buf), sent, "thirst sync payload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkTablePayloadRoundTrips(GameTestHelper helper)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ThirstValuesSyncMessage sent = ThirstValuesSyncMessage.fromTables();
        helper.assertTrue(sent.drinks().containsKey(Items.POTION), "the resolved drink table has no water bottle");
        sent.encode(buf);
        ThirstValuesSyncMessage received = ThirstValuesSyncMessage.decode(buf);
        assertValueEqual(helper, received.drinks().size(), sent.drinks().size(), "drinks sent");
        assertValueEqual(helper, received.foods().size(), sent.foods().size(), "foods sent");
        assertValueEqual(helper, received.drinks().get(Items.POTION)[0], sent.drinks().get(Items.POTION)[0], "water bottle thirst");
        assertValueEqual(helper, received.defaultPurity(), sent.defaultPurity(), "defaultPurity sent");
        assertValueEqual(helper, received.pureThirstBonus(), sent.pureThirstBonus(), "pureWater.thirstBonus sent");
        assertValueEqual(helper, received.pureQuenchedBonus(), sent.pureQuenchedBonus(), "pureWater.quenchedBonus sent");
        helper.succeed();
    }

    private static boolean parse(String json)
    {
        return DrinkValues.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().isPresent();
    }
}
