package com.darkona.droplets.compat.create;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SandFilterBlock extends Block implements IWrenchable, IBE<SandFilterBlockEntity> {

    public SandFilterBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState p_220053_1_, @NotNull BlockGetter p_220053_2_,
                                        @NotNull BlockPos p_220053_3_, @NotNull CollisionContext p_220053_4_)
    {
        return AllShapes.SPOUT;
    }

    @Override
    public void setPlacedBy(@NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull BlockState pState,
                            LivingEntity pPlacer, @NotNull ItemStack pStack)
    {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
    }

    /**
     * Keeps the tanks in the dropped item (pickaxe or wrench); the block item loads them back when placed.
     */
    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params)
    {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SandFilterBlockEntity filter && filter.hasFluid())
            for (ItemStack drop : drops)
                if (drop.is(asItem()))
                    filter.saveToItem(drop, params.getLevel().registryAccess());
        return drops;
    }

    @Override
    public Class<SandFilterBlockEntity> getBlockEntityClass() {
        return SandFilterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SandFilterBlockEntity> getBlockEntityType()
    {
        return CreateRegistry.SAND_FILTER_BE.get();
    }

}
