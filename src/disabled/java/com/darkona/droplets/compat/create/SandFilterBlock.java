package com.darkona.droplets.compat.create;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@link #FACING} is where purified water leaves (down by default); dirty water enters from the opposite side. Placed
 * facing down as before; Create's wrench turns it (the same property as Create's directional blocks).
 */
public class SandFilterBlock extends Block implements IWrenchable, IBE<SandFilterBlockEntity> {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE_X = Shapes.or(Block.box(2, 1, 1, 14, 15, 15), Block.box(0, 2, 2, 16, 14, 14));
    private static final VoxelShape SHAPE_Z = Shapes.or(Block.box(1, 1, 2, 15, 15, 14), Block.box(2, 2, 0, 14, 14, 16));

    public SandFilterBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level,
                                        @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            case Y -> AllShapes.SPOUT;
        };
    }

    @Override
    protected @NotNull BlockState rotate(@NotNull BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected @NotNull BlockState mirror(@NotNull BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
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
