package com.darkona.droplets.foundation.network;

import com.darkona.droplets.content.thirst.ThirstHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientNetworkEvents
{
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggedOutEvent event)
    {
        if (ThirstHelper.hasServerTables())
            ThirstHelper.clearServerTables();
    }
}
