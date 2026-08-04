package com.darkona.droplets.content.thirst;

import com.darkona.droplets.api.ExhaustionModifier;
import com.darkona.droplets.content.registry.EffectInit;
import com.darkona.droplets.core.NumberRows;
import com.darkona.droplets.foundation.config.GameplayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The part of the thirst loss multiplier that depends on the surroundings, armor and effects. {@link PlayerThirst}
 * caches it (recomputed every second or after a change) and multiplies it by the {@code blue_droplets:thirst_drain}
 * attribute each tick. Modifiers registered through the API run last, in id order.
 */
public final class ExhaustionFactors
{
    public static final String[] FACTORS = {"climate", "fire protection", "fire resistance", "rain/thunder", "day/night", "sun", "altitude", "water", "hydrated", "other mods"};

    private static final NumberRows ALTITUDE = new NumberRows(3);
    private static final Map<Identifier, ExhaustionModifier> REGISTERED = new TreeMap<>();
    private static volatile ExhaustionModifier[] modifiers = new ExhaustionModifier[0];

    public static synchronized void register(Identifier id, ExhaustionModifier modifier)
    {
        REGISTERED.put(Objects.requireNonNull(id), Objects.requireNonNull(modifier));
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
        float time = !dayCycle ? 1.0F : (level.isBrightOutside() ? GameplayConfig.DAY_MULTIPLIER : GameplayConfig.NIGHT_MULTIPLIER).get().floatValue();
        float sun = dayCycle && level.isBrightOutside() && !level.isRaining() && level.canSeeSky(eyes) ? GameplayConfig.SUN_MULTIPLIER.get().floatValue() : 1.0F;
        float altitude = (float) NumberRows.band(ALTITUDE.get(GameplayConfig.ALTITUDE_MULTIPLIERS.get()), player.getBlockY() - level.getSeaLevel(), 1.0);
        float water = player.isUnderWater() ? GameplayConfig.UNDERWATER_MULTIPLIER.get().floatValue() * underwaterBreathing(player)
                : player.isInWater() ? GameplayConfig.IN_WATER_MULTIPLIER.get().floatValue() : 1.0F;
        MobEffectInstance hydratedEffect = player.getEffect(EffectInit.HYDRATED);
        float hydrated = hydratedEffect == null ? 1.0F : (float) Math.pow(GameplayConfig.HYDRATED_MULTIPLIER.get(), hydratedEffect.getAmplifier() + 1);

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
            breakdown[8] = hydrated;
        }
        float own = climate * fireProtection * fireResistance * weather * time * sun * altitude * water * hydrated;
        float result = own;
        for (ExhaustionModifier modifier : modifiers)
            result = Math.max(0.0F, modifier.apply(player, result));
        if (breakdown != null)
            breakdown[9] = own == 0.0F ? 1.0F : result / own;
        return result;
    }

    private static float underwaterBreathing(Player player)
    {
        return GameplayConfig.WATER_BREATHING_REDUCES_THIRST.get() && (player.hasEffect(MobEffects.WATER_BREATHING) || player.hasEffect(MobEffects.CONDUIT_POWER))
                ? GameplayConfig.UNDERWATER_BREATHING_MULTIPLIER.get().floatValue() : 1.0F;
    }
}
