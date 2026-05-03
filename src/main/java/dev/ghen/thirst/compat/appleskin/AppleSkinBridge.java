package dev.ghen.thirst.compat.appleskin;

import net.neoforged.bus.api.IEventBus;

final class AppleSkinBridge
{
    private AppleSkinBridge() {}

    static void initClient(IEventBus modBus)
    {
        HUDOverlayHandler.init();
        TooltipOverlayHandler.init();
        modBus.addListener(TooltipOverlayHandler::register);
        modBus.addListener(OverlayRegister::onRenderGuiOverlayPost);
    }
}
