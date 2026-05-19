package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.foundation.config.PurityConfig;
import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the purity property; rain and dripstone adding a level mix in {@code rainCauldronPurity}/{@code dripstoneCauldronPurity}.
 */
@Mixin(LayeredCauldronBlock.class)
public abstract class MixinLayeredCauldronBlock
{
    @Inject(method = "createBlockStateDefinition", at = @At("HEAD"))
    protected void addPurityBlockState(StateDefinition.Builder<Block, BlockState> p_153549_, CallbackInfo ci)
    {
        p_153549_.add(WaterPurity.BLOCK_PURITY);
    }

    @WrapOperation(method = "handlePrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean bluedroplets$rainPurity(Level level, BlockPos pos, BlockState filled, Operation<Boolean> original, @Local(argsOnly = true) BlockState previous)
    {
        return original.call(level, pos, WaterPurity.naturalFill(previous, filled, PurityConfig.RAIN_CAULDRON_PURITY.get()));
    }

    @WrapOperation(method = "receiveStalactiteDrip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean bluedroplets$dripstonePurity(Level level, BlockPos pos, BlockState filled, Operation<Boolean> original, @Local(argsOnly = true) BlockState previous)
    {
        return original.call(level, pos, WaterPurity.naturalFill(previous, filled, PurityConfig.DRIPSTONE_CAULDRON_PURITY.get()));
    }
}
