package com.darkona.droplets.foundation.mixin;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The hunger exhaustion, which {@code thirst.mode = MIRROR_FOOD} follows; Minecraft 26.1 has no getter for it.
 */
@Mixin(FoodData.class)
public interface FoodDataAccessor
{
    @Accessor("exhaustionLevel")
    float blue_droplets$exhaustionLevel();
}
