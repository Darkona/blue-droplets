package com.darkona.dropletsofthirst.api;

/**
 * What an item gives when drunk or eaten, in points (2 points = 1 droplet on the HUD). Negative values (salty food and
 * drinks, -20 to 20 for thirst) remove thirst and quenched.
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

    /**
     * Thirst points it removes ({@code -thirst}), or 0 when it is not salty; 2 points = 1 droplet on the HUD.
     */
    public int saltiness()
    {
        return Math.max(0, -thirst);
    }
}
