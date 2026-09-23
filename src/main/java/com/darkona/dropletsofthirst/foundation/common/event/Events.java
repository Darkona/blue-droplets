package com.darkona.dropletsofthirst.foundation.common.event;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

/**
 * Posting on the Forge bus the way later versions do: {@code post} returns the event, so callers read the values
 * listeners left and {@code isCanceled()} from it.
 */
public final class Events
{
    private Events() {}

    public static <E extends Event> E post(E event)
    {
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }
}
