package com.darkona.droplets.content.thirst;

import com.darkona.droplets.api.ExhaustionModifier;
import com.darkona.droplets.core.NumberRows;
import com.darkona.droplets.foundation.config.GameplayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/**
 * The part of the thirst loss multiplier that depends on the surroundings, armor and effects. {@link PlayerThirst}
 * caches it (recomputed every second or after a change) and multiplies it by the {@code bluedroplets:thirst_drain}
 * attribute each tick. Modifiers registered through the API run last, in id order.
 */
public final class ExhaustionFactors
{
    public static final String[] FACTORS = {"climate", "fire protection", "fire resistance", "rain/thunder", "day/night", "sun", "altitude", "water", "other mods"};

    private static final NumberRows ALTITUDE = new NumberRows(3);
    private static final Map<ResourceLocation, ExhaustionModifier> REGISTERED = new TreeMap<>();
    private static volatile ExhaustionModifier[] modifiers = new ExhaustionModifier[0];

    public static synchronized void register(ResourceLocation id, ExhaustionModifier modifier)
    {
        REGISTERED.put(id, modifier);
        modifiers = REGISTERED.values().toArray(new ExhaustionModifier[0]);
    }

    private ExhaustionFactors() {}

    /**
     * Product of the factors, then passed through the registered modifiers; when {@code breakdown} is given (length
     * {@link #FACTORS}) each factor is written to it, the modifiers as one ratio.
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
        float own = climate * fireProtection * fireResistance * weather * time * sun * altitude * water;
        float result = own;
        for (ExhaustionModifier modifier : modifiers)
            result = Math.max(0.0F, modifier.apply(player, result));
        if (breakdown != null)
            breakdown[8] = own == 0.0F ? 1.0F : result / own;
        return result;
    }
}
