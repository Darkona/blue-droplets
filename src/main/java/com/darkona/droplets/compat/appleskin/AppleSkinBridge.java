package com.darkona.droplets.compat.appleskin;

import net.neoforged.bus.api.IEventBus;

final class AppleSkinBridge
{
    private AppleSkinBridge() {}

    static void initClient(IEventBus modBus)
    {
        TooltipOverlayHandler.init();
        modBus.addListener(TooltipOverlayHandler::register);
    }
}
