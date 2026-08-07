package com.darkona.droplets.compat.delight;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Added to Extra Delight's infinite water tank ({@code WellFluidCapability}) so its water can take the purity of the
 * world where its tap or sink stands.
 */
public interface WaterSourceTank
{
    void blue_droplets$setSource(BlockEntity source);
}
