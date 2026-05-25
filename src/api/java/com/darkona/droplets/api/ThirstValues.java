package com.darkona.droplets.api;

/**
 * What an item gives when drunk or eaten.
 *
 * @param purity    purity used when the stack stores none, or {@link DropletsAPI#NO_PURITY}
 * @param estimated whether the values were estimated from recipes rather than given by a config, datapack or mod
 */
public record ThirstValues(int thirst, int quenched, int purity, boolean estimated)
{
    public ThirstValues(int thirst, int quenched, int purity)
    {
        this(thirst, quenched, purity, false);
    }

    public ThirstValues(int thirst, int quenched)
    {
        this(thirst, quenched, DropletsAPI.NO_PURITY, false);
    }
}
