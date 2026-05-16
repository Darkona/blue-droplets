package com.darkona.droplets.content.data;

import com.darkona.droplets.content.purity.WaterPurity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Value of the {@code bluedroplets:dimension_water} dimension type data map: {@code base} is the base purity of water
 * in biomes without their own.
 */
public record DimensionWater(Optional<Integer> base)
{
    public static final Codec<DimensionWater> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(WaterPurity.MIN_PURITY, WaterPurity.MAX_PURITY).optionalFieldOf("base").forGetter(DimensionWater::base)
    ).apply(instance, DimensionWater::new));
}
