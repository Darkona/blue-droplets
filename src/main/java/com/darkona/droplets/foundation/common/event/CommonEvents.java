package com.darkona.droplets.foundation.common.event;

import com.darkona.droplets.compat.create.SandFilterBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber
public class CommonEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if(ModList.get().isLoaded("create")) {
            SandFilterBlockEntity.registerCapabilities(event);
        }
    }
}
