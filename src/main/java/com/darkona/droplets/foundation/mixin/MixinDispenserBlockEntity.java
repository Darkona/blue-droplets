package com.darkona.droplets.foundation.mixin;

import com.darkona.droplets.content.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A container a dispenser filled from a stack of empty ones goes back in with {@code addItem}; see
 * {@link MixinDispenserBlock}.
 */
@Mixin(DispenserBlockEntity.class)
public abstract class MixinDispenserBlockEntity extends BlockEntity
{
    private MixinDispenserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Inject(method = "addItem", at = @At("HEAD"))
    private void blue_droplets$worldWaterPurity(ItemStack stack, CallbackInfoReturnable<Integer> cir)
    {
        if (level != null && !level.isClientSide())
            WaterPurity.dispenserFilled(level, worldPosition, getBlockState(), stack);
    }
}
