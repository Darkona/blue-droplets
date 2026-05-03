package dev.ghen.thirst.compat.appleskin;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

public final class AppleSkinCompat
{
    public static final boolean LOADED = ModList.get().isLoaded("appleskin");

    private AppleSkinCompat() {}

    public static void initClient(IEventBus modBus)
    {
        if (LOADED)
            AppleSkinBridge.initClient(modBus);
    }
}
