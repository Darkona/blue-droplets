package com.darkona.droplets.content.data;

import com.darkona.droplets.content.purity.WaterPurity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * Value of the {@code blue_droplets:biome_water} biome data map: {@code base} replaces the base purity of the biome's
 * water, {@code delta} is added after the other sources, and the result never exceeds {@code max}.
 */
public record BiomeWater(Optional<Integer> base, int delta, int max)
{
    public static final Codec<BiomeWater> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(WaterPurity.MIN_PURITY, WaterPurity.MAX_PURITY).optionalFieldOf("base").forGetter(BiomeWater::base),
            Codec.intRange(-WaterPurity.MAX_PURITY, WaterPurity.MAX_PURITY).optionalFieldOf("delta", 0).forGetter(BiomeWater::delta),
            Codec.intRange(WaterPurity.MIN_PURITY, WaterPurity.MAX_PURITY).optionalFieldOf("max", WaterPurity.MAX_PURITY).forGetter(BiomeWater::max)
    ).apply(instance, BiomeWater::new));
}
