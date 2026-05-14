package com.darkona.droplets.foundation.network;

import com.darkona.droplets.api.ThirstHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientNetworkEvents
{
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        if (ThirstHelper.hasServerTables())
            ThirstHelper.clearServerTables();
    }
}
