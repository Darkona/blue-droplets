package com.darkona.dropletsofthirst.foundation.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Vanilla's temperature of a biome at a position, cooled with height and cached per thread, which is private in
 * {@link Biome}; the climate of thirst reads it, as Serene Seasons does.
 */
@Mixin(Biome.class)
public interface BiomeAccessor
{
    @Invoker("getTemperature")
    float droplets_of_thirst$getTemperature(BlockPos pos, int seaLevel);
}
