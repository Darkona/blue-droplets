package com.darkona.dropletsofthirst.content.data;

import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.darkona.dropletsofthirst.core.ThirstConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Value of the {@code droplets_of_thirst:drinks} item data map. {@code purity} is the purity of the drink when the stack
 * stores none; without it, drinks that are not water containers roll no purity effects. Negative values (salty food)
 * remove thirst and quenched.
 */
public record DrinkValues(int thirst, int quenched, Optional<Integer> purity)
{
    public static final Codec<DrinkValues> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(-ThirstConstants.MAX_THIRST, ThirstConstants.MAX_THIRST).fieldOf("thirst").forGetter(DrinkValues::thirst),
            Codec.intRange(-ThirstConstants.MAX_THIRST, Integer.MAX_VALUE).fieldOf("quenched").forGetter(DrinkValues::quenched),
            Codec.intRange(WaterPurity.MIN_PURITY, WaterPurity.MAX_PURITY).optionalFieldOf("purity").forGetter(DrinkValues::purity)
    ).apply(instance, DrinkValues::new));
}
