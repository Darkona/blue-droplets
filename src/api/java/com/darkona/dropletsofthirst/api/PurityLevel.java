package com.darkona.dropletsofthirst.api;

import org.jetbrains.annotations.Nullable;

/**
 * The six levels of water purity, from {@link #CONTAMINATED} (0) to {@link #PURE} (5). Stacks, fluids, events and the
 * config store the level as an int; this enum names them.
 */
public enum PurityLevel
{
    CONTAMINATED("contaminated", 0x8B5A2B),
    DIRTY("dirty", 0xB08A5A),
    MURKY("murky", 0x8FA07E),
    ACCEPTABLE("acceptable", 0x3F86D8),
    CLEAN("clean", 0x7EC8F2),
    PURE("pure", 0xE6F6FF);

    /** Lowest level, {@link #CONTAMINATED}. */
    public static final int MIN = 0;
    /** Highest level, {@link #PURE}. */
    public static final int MAX = 5;

    private static final PurityLevel[] BY_LEVEL = values();

    private final String id;
    private final int color;

    PurityLevel(String id, int color)
    {
        this.id = id;
        this.color = color;
    }

    /** The level stored on stacks and fluids, {@link #MIN} to {@link #MAX}. */
    public int level()
    {
        return ordinal();
    }

    /** Lowercase name, as in config keys and the {@code droplets_of_thirst.purity.<id>} translation key. */
    public String id()
    {
        return id;
    }

    public String translationKey()
    {
        return DropletsAPI.MOD_ID + ".purity." + id;
    }

    /** Color of the level's name in tooltips, {@code 0xRRGGBB}. */
    public int color()
    {
        return color;
    }

    public static boolean isValid(int level)
    {
        return level >= MIN && level <= MAX;
    }

    /**
     * The level with this number, or null outside {@link #MIN}..{@link #MAX}.
     */
    public static @Nullable PurityLevel byLevel(int level)
    {
        return isValid(level) ? BY_LEVEL[level] : null;
    }
}
