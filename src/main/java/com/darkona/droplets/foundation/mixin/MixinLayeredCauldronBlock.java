package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.foundation.config.PurityConfig;
import com.darkona.droplets.content.purity.WaterPurity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the purity property; rain and dripstone adding a level mix in {@code rainCauldronPurity}/{@code dripstoneCauldronPurity};
 * water cauldrons on a heat source boil ({@code cauldron.boiling}).
 */
@Mixin(LayeredCauldronBlock.class)
public abstract class MixinLayeredCauldronBlock extends AbstractCauldronBlock
{
    private MixinLayeredCauldronBlock(Properties properties, CauldronInteraction.InteractionMap interactions)
    {
        super(properties, interactions);
    }

    /**
     * Cached per state by {@code BlockStateBase#initCache} and counted by chunk sections, so it must depend on the state only.
     */
    @Override
    protected boolean isRandomlyTicking(BlockState state)
    {
        return WaterPurity.canBoil(state) || super.isRandomlyTicking(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        super.randomTick(state, level, pos, random);
        WaterPurity.boil(state, level, pos, random);
    }

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
