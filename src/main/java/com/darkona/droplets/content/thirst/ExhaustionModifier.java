package com.darkona.droplets.content.thirst;

import com.darkona.droplets.api.ThirstHelper;
import com.darkona.droplets.core.NumberRows;
import com.darkona.droplets.foundation.config.GameplayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The part of the thirst loss multiplier that depends on the surroundings, armor and effects. {@link PlayerThirst}
 * caches it (recomputed every second or after a change) and multiplies it by the {@code bluedroplets:thirst_drain}
 * attribute each tick.
 */
public final class ExhaustionModifier
{
    public static final String[] FACTORS = {"climate", "fire protection", "fire resistance", "rain/thunder", "day/night", "sun", "altitude", "water"};

    private static final NumberRows ALTITUDE = new NumberRows(3);

    private ExhaustionModifier() {}

    /**
     * Product of all factors; when {@code breakdown} is given (length {@link #FACTORS}) each factor is written to it.
     */
    public static float compute(Player player, @Nullable float[] breakdown)
    {
        Level level = player.level();
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        boolean dayCycle = !level.dimensionType().hasFixedTime();

        float climate = ThirstHelper.getExhaustionBiomeModifier(player);
        float fireProtection = ThirstHelper.getExhaustionFireProtModifier(player);
        float fireResistance = ThirstHelper.getExhaustionFireResistanceModifier(player);
        float weather = !level.isRainingAt(eyes) ? 1.0F
                : (level.isThundering() ? GameplayConfig.THUNDER_MULTIPLIER : GameplayConfig.RAIN_MULTIPLIER).get().floatValue();
        float time = !dayCycle ? 1.0F : (level.isDay() ? GameplayConfig.DAY_MULTIPLIER : GameplayConfig.NIGHT_MULTIPLIER).get().floatValue();
        float sun = dayCycle && level.isDay() && !level.isRaining() && level.canSeeSky(eyes) ? GameplayConfig.SUN_MULTIPLIER.get().floatValue() : 1.0F;
        float altitude = (float) NumberRows.band(ALTITUDE.get(GameplayConfig.ALTITUDE_MULTIPLIERS.get()), player.getBlockY() - level.getSeaLevel(), 1.0);
        float water = player.isUnderWater() ? GameplayConfig.UNDERWATER_MULTIPLIER.get().floatValue()
                : player.isInWater() ? GameplayConfig.IN_WATER_MULTIPLIER.get().floatValue() : 1.0F;

        if (breakdown != null)
        {
            breakdown[0] = climate;
            breakdown[1] = fireProtection;
            breakdown[2] = fireResistance;
            breakdown[3] = weather;
            breakdown[4] = time;
            breakdown[5] = sun;
            breakdown[6] = altitude;
            breakdown[7] = water;
        }
        return climate * fireProtection * fireResistance * weather * time * sun * altitude * water;
    }
}
