package com.darkona.droplets.foundation.config;

/**
 * Common config values that clients must share with the server. On a client of a remote server they hold the
 * server's values (sent with the drink tables); everywhere else, and before any server sent them, the local config.
 */
public final class SyncedValues
{
    private static volatile int defaultPurity = -1;
    private static volatile int waterBottleStackSize = -1;
    private static volatile int purityEnabled = -1;
    private static volatile int canFillFromFlowingWater = -1;
    private static volatile int purifiedThirstBonus = -1;
    private static volatile int purifiedQuenchedBonus = -1;

    private SyncedValues() {}

    public static int defaultPurity()
    {
        int value = defaultPurity;
        return value >= 0 ? value : PurityConfig.DEFAULT_PURITY.get();
    }

    public static int waterBottleStackSize()
    {
        int value = waterBottleStackSize;
        return value > 0 ? value : GameplayConfig.WATER_BOTTLE_STACK_SIZE.get();
    }

    public static boolean purityEnabled()
    {
        int value = purityEnabled;
        return value >= 0 ? value == 1 : PurityConfig.ENABLED.get();
    }

    public static boolean canFillFromFlowingWater()
    {
        int value = canFillFromFlowingWater;
        return value >= 0 ? value == 1 : GameplayConfig.CAN_FILL_FROM_FLOWING_WATER.get();
    }

    public static int purifiedThirstBonus()
    {
        int value = purifiedThirstBonus;
        return value >= 0 ? value : PurityConfig.PURIFIED_THIRST_BONUS.get();
    }

    public static int purifiedQuenchedBonus()
    {
        int value = purifiedQuenchedBonus;
        return value >= 0 ? value : PurityConfig.PURIFIED_QUENCHED_BONUS.get();
    }

    public static void useServerValues(int defaultPurity, int waterBottleStackSize, boolean purityEnabled, boolean canFillFromFlowingWater,
                                       int purifiedThirstBonus, int purifiedQuenchedBonus)
    {
        SyncedValues.defaultPurity = defaultPurity;
        SyncedValues.waterBottleStackSize = waterBottleStackSize;
        SyncedValues.purityEnabled = purityEnabled ? 1 : 0;
        SyncedValues.canFillFromFlowingWater = canFillFromFlowingWater ? 1 : 0;
        SyncedValues.purifiedThirstBonus = purifiedThirstBonus;
        SyncedValues.purifiedQuenchedBonus = purifiedQuenchedBonus;
    }

    public static void clear()
    {
        defaultPurity = -1;
        waterBottleStackSize = -1;
        purityEnabled = -1;
        canFillFromFlowingWater = -1;
        purifiedThirstBonus = -1;
        purifiedQuenchedBonus = -1;
    }
}
