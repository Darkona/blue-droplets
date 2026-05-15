package com.darkona.droplets.core;

/**
 * Gameplay numbers that are not configurable yet (E18). Defaults match the current behaviour.
 */
public final class ThirstConstants
{
    public static final int MAX_THIRST = 20;
    public static final int RESPAWN_THIRST = 20;
    public static final int RESPAWN_QUENCHED = 5;

    public static final float EXHAUSTION_PER_POINT = 4.0F;
    public static final float NAUSEA_EXHAUSTION_PER_TICK = 0.06F;
    public static final float HUNGER_EXHAUSTION_PER_LEVEL = 0.005F;

    public static final int PASSIVE_REGEN_INTERVAL_TICKS = 10;
    /** The climate/armor/effect exhaustion modifier is recomputed this often (staggered per player). */
    public static final int MODIFIER_INTERVAL_TICKS = 20;
    /** Exhaustion is sent to the client in steps of 1/this (AppleSkin underlay: about 2 px per step). */
    public static final int EXHAUSTION_SYNC_STEPS = 10;
    /** Full resync even without changes, in case the client lost its copy. */
    public static final int SAFETY_RESYNC_TICKS = 200;
    public static final int PEACEFUL_REGEN_AMOUNT = 1;

    public static final float RAIN_MAX_PITCH = -80.0F;
    public static final int RAIN_THIRST = 1;
    public static final int RAIN_QUENCHED = 1;

    public static final float DAMAGE_AMOUNT = 1.0F;
    public static final int DAMAGE_INTERVAL_TICKS = 40;
    /** Dehydration stops hurting at or below this health, like vanilla starvation. Hard has no floor. */
    public static final float EASY_MIN_HEALTH = 10.0F;
    public static final float NORMAL_MIN_HEALTH = 1.0F;

    /** Sprinting needs more thirst than this, like vanilla's food check. */
    public static final int SPRINT_MIN_THIRST = 6;

    public static final int FULL_REGEN_MIN_THIRST = 20;
    public static final int SLOW_REGEN_MIN_THIRST = 19;
    public static final int SLOW_REGEN_INTERVAL_TICKS = 8;
    public static final int HUNGER_REGEN_MIN_THIRST = 19;

    public static final float FIRE_PROTECTION_REDUCTION_PER_LEVEL = 0.0625F * 0.75F;
    public static final int FIRE_PROTECTION_MAX_LEVELS = 12;

    private ThirstConstants() {}
}
