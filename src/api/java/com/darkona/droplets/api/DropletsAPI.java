package com.darkona.droplets.api;

import com.darkona.droplets.api.spi.DropletsService;

import java.util.Objects;

/**
 * Blue Droplets: thirst, quenched and water purity for players.
 * <p>
 * <b>Sides.</b> Reading works on both sides (clients see their own synced values). Everything that changes thirst is
 * server side: on a client it does nothing and returns {@code false}.
 * <p>
 * <b>Soft dependency.</b> Compile against {@code bluedroplets-api} only and guard calls with
 * {@code ModList.get().isLoaded("bluedroplets")}.
 */
public final class DropletsAPI
{
    public static final String MOD_ID = "bluedroplets";
    /** Bumped when the API changes incompatibly. */
    public static final int API_VERSION = 1;

    public static final int MAX_THIRST = 20;
    public static final int NO_PURITY = -1;
    public static final int DIRTY = 0;
    public static final int SLIGHTLY_DIRTY = 1;
    public static final int ACCEPTABLE = 2;
    public static final int PURIFIED = 3;

    private static DropletsService service;

    private DropletsAPI() {}

    private static DropletsService service()
    {
        return Objects.requireNonNull(service, "Blue Droplets is not loaded");
    }

    /**
     * Internal: Blue Droplets installs its implementation here while it is constructed.
     */
    public static void setService(DropletsService implementation)
    {
        if (service != null)
            throw new IllegalStateException("The Blue Droplets service is already set");
        service = implementation;
    }
}
