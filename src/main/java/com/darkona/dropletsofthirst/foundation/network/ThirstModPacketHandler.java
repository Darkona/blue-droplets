package com.darkona.dropletsofthirst.foundation.network;

import com.darkona.dropletsofthirst.DropletsOfThirst;
import com.darkona.dropletsofthirst.foundation.network.message.DrinkByHandMessage;
import com.darkona.dropletsofthirst.foundation.network.message.PlayerThirstSyncMessage;
import com.darkona.dropletsofthirst.foundation.network.message.ThirstValuesSyncMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Payloads, on one channel; registered from the mod constructor.
 */
public class ThirstModPacketHandler
{
    private static final String PROTOCOL_VERSION = "0.1.12";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(DropletsOfThirst.asResource("main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(DrinkByHandMessage.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DrinkByHandMessage::encode)
                .decoder(DrinkByHandMessage::decode)
                .consumer(mainThread(DrinkByHandMessage::serverHandle))
                .add();
        CHANNEL.messageBuilder(ThirstValuesSyncMessage.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ThirstValuesSyncMessage::encode)
                .decoder(ThirstValuesSyncMessage::decode)
                .consumer(mainThread(ThirstValuesSyncMessage::clientHandle))
                .add();
        CHANNEL.messageBuilder(PlayerThirstSyncMessage.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayerThirstSyncMessage::encode)
                .decoder(PlayerThirstSyncMessage::decode)
                .consumer(mainThread(PlayerThirstSyncMessage::clientHandle))
                .add();
    }

    /**
     * Forge 1.18.2 has no {@code consumerMainThread}: the handler runs on the main thread and the packet is marked handled.
     */
    private static <MSG> BiConsumer<MSG, Supplier<NetworkEvent.Context>> mainThread(BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        return (message, context) -> {
            context.get().enqueueWork(() -> handler.accept(message, context));
            context.get().setPacketHandled(true);
        };
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
