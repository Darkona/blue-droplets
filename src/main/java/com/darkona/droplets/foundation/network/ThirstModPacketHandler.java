package com.darkona.droplets.foundation.network;

import com.darkona.droplets.foundation.network.message.DrinkByHandMessage;
import com.darkona.droplets.foundation.network.message.PlayerThirstSyncMessage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber
public class ThirstModPacketHandler
{
    private static final String PROTOCOL_VERSION = "0.1.5";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                DrinkByHandMessage.TYPE,
                DrinkByHandMessage.STREAM_CODEC,
                DrinkByHandMessage::serverHandle
        );
        registrar.playBidirectional(
                PlayerThirstSyncMessage.TYPE,
                PlayerThirstSyncMessage.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        PlayerThirstSyncMessage::clientHandle,
                        PlayerThirstSyncMessage::serverHandle
                )
        );
    }
}
