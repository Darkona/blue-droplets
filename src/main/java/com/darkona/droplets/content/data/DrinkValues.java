package com.darkona.droplets.content.data;

import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.core.ThirstConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

import java.util.Optional;

/**
 * Value of the {@code bluedroplets:drinks} item data map. {@code purity} is the purity of the drink when the stack
 * stores none; without it, drinks that are not water containers roll no purity effects.
 */
public record DrinkValues(int thirst, int quenched, Optional<Integer> purity)
{
    public static final Codec<DrinkValues> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, ThirstConstants.MAX_THIRST).fieldOf("thirst").forGetter(DrinkValues::thirst),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("quenched").forGetter(DrinkValues::quenched),
            Codec.intRange(WaterPurity.MIN_PURITY, WaterPurity.MAX_PURITY).optionalFieldOf("purity").forGetter(DrinkValues::purity)
    ).apply(instance, DrinkValues::new));
}
