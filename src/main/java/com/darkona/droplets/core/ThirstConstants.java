package com.darkona.droplets.core;

/**
 * Fixed numbers that are not gameplay knobs (those are in {@code gameplay.toml}).
 */
public final class ThirstConstants
{
    public static final int MAX_THIRST = 20;
    /** Thirst and quenched of a new player. */
    public static final int RESPAWN_THIRST = 20;
    public static final int RESPAWN_QUENCHED = 5;

    /** Food exhaustion per tick and level of the Hunger effect (vanilla), taken back out of the mirrored exhaustion. */
    public static final float HUNGER_EXHAUSTION_PER_LEVEL = 0.005F;

    /** The climate/armor/effect exhaustion modifier is recomputed this often (staggered per player). */
    public static final int MODIFIER_INTERVAL_TICKS = 20;
    /** Exhaustion is sent to the client in steps of 1/this (AppleSkin underlay: about 2 px per step). */
    public static final int EXHAUSTION_SYNC_STEPS = 10;
    /** Full resync even without changes, in case the client lost its copy. */
    public static final int SAFETY_RESYNC_TICKS = 200;

    /** {@code sprint.minThirst} on the client until the server sends its value. */
    public static final int SPRINT_MIN_THIRST = 6;

    private ThirstConstants() {}
}
