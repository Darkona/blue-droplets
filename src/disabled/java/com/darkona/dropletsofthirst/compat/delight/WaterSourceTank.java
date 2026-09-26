package com.darkona.dropletsofthirst.compat.delight;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Added to Extra Delight's infinite water tank ({@code WellFluidCapability}) so its water can take the purity of the
 * world where its tap or sink stands.
 */
public interface WaterSourceTank
{
    void droplets_of_thirst$setSource(BlockEntity source);
}
