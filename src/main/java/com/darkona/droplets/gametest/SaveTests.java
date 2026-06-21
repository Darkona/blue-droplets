package com.darkona.droplets.gametest;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.event.ThirstChangeEvent;
import com.darkona.droplets.content.data.DrinkValues;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.content.registry.ThirstComponent;
import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.foundation.network.message.PlayerThirstSyncMessage;
import com.darkona.droplets.foundation.network.message.ThirstValuesSyncMessage;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import static com.darkona.droplets.gametest.TestSupport.player;
import static com.darkona.droplets.gametest.TestSupport.thirst;

/**
 * Saved data and codecs: the player attachment, the purity component on disk and on the wire, the drinks data map and
 * the payloads the HUD is fed from.
 */
@GameTestHolder(BlueDroplets.ID)
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
        CompoundTag tag = thirst.serializeNBT(helper.getLevel().registryAccess());
        PlayerThirst loaded = new PlayerThirst();
        loaded.deserializeNBT(helper.getLevel().registryAccess(), tag);
        helper.assertValueEqual(loaded.getThirst(), 13, "thirst after a save");
        helper.assertValueEqual(loaded.getQuenched(), 7, "quenched after a save");
        helper.assertValueEqual(loaded.getExhaustion(), 2.5F, "exhaustion after a save");
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
        loaded.deserializeNBT(helper.getLevel().registryAccess(), tag);
        helper.assertValueEqual(loaded.getQuenched(), 10, "quenched above thirst in an old save");
        helper.assertTrue(loaded.getShouldTickThirst(), "a save without the enable flag is disabled");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purityComponentSurvivesASave(GameTestHelper helper)
    {
        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemStack bucket = WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), 1);
        Tag saved = ItemStack.CODEC.encodeStart(ops, bucket).getOrThrow();
        ItemStack loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        helper.assertValueEqual(loaded.get(ThirstComponent.PURITY), 1, "purity after a save");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidPurityLoadsAsDefault(GameTestHelper helper)
    {
        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        bucket.set(ThirstComponent.PURITY, 7);
        Tag saved = ItemStack.CODEC.encodeStart(ops, bucket).getOrThrow();
        ItemStack loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        helper.assertTrue(loaded.is(Items.WATER_BUCKET), "a bucket with purity 7 was lost on load");
        helper.assertValueEqual(loaded.get(ThirstComponent.PURITY), WaterPurity.defaultPurity(), "purity 7 after a load");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void purityComponentCrossesTheNetwork(GameTestHelper helper)
    {
        RegistryFriendlyByteBuf buf = buffer(helper.getLevel().registryAccess());
        ItemStack.STREAM_CODEC.encode(buf, WaterPurity.addPurity(new ItemStack(Items.WATER_BUCKET), 1));
        ItemStack received = ItemStack.STREAM_CODEC.decode(buf);
        helper.assertValueEqual(received.get(ThirstComponent.PURITY), 1, "purity after the network");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinksDataMapAcceptsSaltyAndRejectsOutOfRange(GameTestHelper helper)
    {
        helper.assertTrue(parse("{\"thirst\": -2, \"quenched\": -2}"), "salty entry rejected");
        helper.assertTrue(parse("{\"thirst\": 6, \"quenched\": 8, \"purity\": 3}"), "water entry rejected");
        helper.assertFalse(parse("{\"thirst\": 21, \"quenched\": 0}"), "thirst 21 accepted");
        helper.assertFalse(parse("{\"thirst\": 2, \"quenched\": -30}"), "quenched -30 accepted");
        helper.assertFalse(parse("{\"thirst\": 2, \"quenched\": 2, \"purity\": 4}"), "purity 4 accepted");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void thirstSyncPayloadRoundTrips(GameTestHelper helper)
    {
        RegistryFriendlyByteBuf buf = buffer(helper.getLevel().registryAccess());
        PlayerThirstSyncMessage sent = new PlayerThirstSyncMessage(17, 9, 1.25F, PlayerThirst.SYNC_ENABLED | PlayerThirst.SYNC_SPRINT_BLOCKED | 6 << PlayerThirst.SYNC_SPRINT_MIN_SHIFT);
        PlayerThirstSyncMessage.STREAM_CODEC.encode(buf, sent);
        helper.assertValueEqual(PlayerThirstSyncMessage.STREAM_CODEC.decode(buf), sent, "thirst sync payload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkTablePayloadRoundTrips(GameTestHelper helper)
    {
        RegistryFriendlyByteBuf buf = buffer(helper.getLevel().registryAccess());
        ThirstValuesSyncMessage sent = ThirstValuesSyncMessage.fromTables();
        helper.assertTrue(sent.drinks().containsKey(Items.POTION), "the resolved drink table has no water bottle");
        ThirstValuesSyncMessage.STREAM_CODEC.encode(buf, sent);
        ThirstValuesSyncMessage received = ThirstValuesSyncMessage.STREAM_CODEC.decode(buf);
        helper.assertValueEqual(received.drinks().size(), sent.drinks().size(), "drinks sent");
        helper.assertValueEqual(received.foods().size(), sent.foods().size(), "foods sent");
        helper.assertValueEqual(received.drinks().get(Items.POTION)[0], sent.drinks().get(Items.POTION)[0], "water bottle thirst");
        helper.assertValueEqual(received.defaultPurity(), sent.defaultPurity(), "defaultPurity sent");
        helper.assertValueEqual(received.purifiedThirstBonus(), sent.purifiedThirstBonus(), "purifiedWater.thirstBonus sent");
        helper.assertValueEqual(received.purifiedQuenchedBonus(), sent.purifiedQuenchedBonus(), "purifiedWater.quenchedBonus sent");
        helper.succeed();
    }

    private static boolean parse(String json)
    {
        return DrinkValues.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().isPresent();
    }

    private static RegistryFriendlyByteBuf buffer(RegistryAccess registries)
    {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
    }
}
