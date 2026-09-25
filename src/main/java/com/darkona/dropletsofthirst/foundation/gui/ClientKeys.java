package com.darkona.dropletsofthirst.foundation.gui;

import net.minecraft.client.gui.screens.Screen;

/**
 * Key state for tooltips; only loaded on the client (callers check the side first).
 */
public final class ClientKeys
{
    private ClientKeys() {}

    public static boolean shiftDown()
    {
        return Screen.hasShiftDown();
    }
}
