package com.darkona.dropletsofthirst.compat.create;

import com.darkona.dropletsofthirst.foundation.config.CompatConfig;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.utility.Lang;
import com.darkona.dropletsofthirst.content.purity.WaterPurity;
import com.simibubi.create.foundation.utility.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

public class SandFilterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation
{
    public static final int TANK_SIZE = 1000;
    SmartFluidTankBehaviour dirtyTank;
    SmartFluidTankBehaviour purifiedTank;
    /** The block in front, recomputed only when the filter is turned. */
    private Direction nextFacing;
    private BlockPos nextPos;
    private final LazyOptional<IFluidHandler> tanksView = LazyOptional.of(() -> new SandFilterTanksView(this));

    public SandFilterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        dirtyTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 1, TANK_SIZE, false);
        behaviours.add(dirtyTank);
        purifiedTank = SmartFluidTankBehaviour.single(this, TANK_SIZE).forbidInsertion();
        behaviours.add(purifiedTank);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().expandTowards(0, -2, 0);
    }

    /**
     * Pipes connect only to the two ends: purified water leaves from the {@code FACING} side, dirty water enters
     * from the opposite one. Without a side, both tanks, read only, for Jade and other inspectors.
     */
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if (cap == ForgeCapabilities.FLUID_HANDLER)
        {
            if (side == null)
                return tanksView.cast();
            Direction facing = getBlockState().getValue(SandFilterBlock.FACING);
            if (side == facing)
                return purifiedTank.getCapability().cast();
            if (side == facing.getOpposite())
                return dirtyTank.getCapability().cast();
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        tanksView.invalidate();
    }

    /**
     * Moves up to {@code sandFilterMbPerTick} from the dirty to the purified tank, only what the purified tank accepts:
     * water of a different purity is not mixed, it waits. Then hands purified water on to a filter right in front
     * that faces the same way, so a row of filters purifies in stages.
     */
    public void tick()
    {
        super.tick();

        if(level.isClientSide())
            return;

        int rate = CompatConfig.SAND_FILTER_MB_PER_TICK.get();
        filter(rate);
        passOn(rate);
    }

    private void filter(int rate)
    {
        SmartFluidTank dirty = dirtyTank.getPrimaryHandler();
        SmartFluidTank purified = purifiedTank.getPrimaryHandler();
        if(dirty.isEmpty() || purified.getSpace() <= 0)
            return;

        FluidStack water = dirty.drain(rate, IFluidHandler.FluidAction.SIMULATE);
        if(water.isEmpty())
            return;

        if(water.getFluid().is(FluidTags.WATER))
            WaterPurity.addPurity(water, filteredPurity(WaterPurity.getPurity(water)));

        int accepted = purified.fill(water, IFluidHandler.FluidAction.SIMULATE);
        if(accepted <= 0)
            return;

        int drained = dirty.drain(accepted, IFluidHandler.FluidAction.EXECUTE).getAmount();
        purified.fill(new FluidStack(water, drained), IFluidHandler.FluidAction.EXECUTE);
    }

    /**
     * {@code sandFilterFiltrationAmount} more, up to {@code sandFilterMaxPurity}; water already above it passes unchanged.
     */
    public static int filteredPurity(int purity)
    {
        return CreateCompat.sandFilterPurity(purity);
    }

    private void passOn(int rate)
    {
        SmartFluidTank purified = purifiedTank.getPrimaryHandler();
        if(purified.isEmpty())
            return;
        Direction facing = getBlockState().getValue(SandFilterBlock.FACING);
        if(facing != nextFacing)
        {
            nextFacing = facing;
            nextPos = worldPosition.relative(facing);
        }
        if(!(level.getBlockEntity(nextPos) instanceof SandFilterBlockEntity next) || next.getBlockState().getValue(SandFilterBlock.FACING) != facing)
            return;
        SmartFluidTank nextDirty = next.dirtyTank.getPrimaryHandler();
        int accepted = nextDirty.fill(purified.drain(rate, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.SIMULATE);
        if(accepted > 0)
            nextDirty.fill(purified.drain(accepted, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
    }

    public boolean hasFluid()
    {
        return !dirtyTank.isEmpty() || !purifiedTank.isEmpty();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking)
    {
        LangBuilder mb = Lang.translate("generic.unit.millibuckets");
        Lang.translate("gui.goggles.fluid_container")
                .forGoggles(tooltip);

        tankTooltip(tooltip, mb, dirtyTank);
        tankTooltip(tooltip, mb, purifiedTank);

        if(dirtyTank.isEmpty() && purifiedTank.isEmpty()){
            Lang.translate("gui.goggles.fluid_container.capacity")
                    .add(Lang.number(dirtyTank.getPrimaryHandler().getTankCapacity(0))
                            .add(mb)
                            .style(ChatFormatting.GOLD))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
        }

        return hasFluid();
    }

    /**
     * The purity and fluid name of a tank, and its amount over its capacity; nothing for an empty tank.
     */
    private static void tankTooltip(List<Component> tooltip, LangBuilder mb, SmartFluidTankBehaviour tank) {
        if(tank.isEmpty())
            return;
        SmartFluidTank handler = tank.getPrimaryHandler();
        Lang.builder()
                .text(WaterPurity.enabled() ? WaterPurity.getPurityText(WaterPurity.getPurity(handler.getFluid())) + " " : "")
                .add(Lang.fluidName(handler.getFluid()))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        Lang.builder()
                .add(Lang.number(handler.getFluidAmount())
                        .add(mb)
                        .style(ChatFormatting.GOLD))
                .text(ChatFormatting.GRAY, " / ")
                .add(Lang.number(handler.getCapacity())
                        .add(mb)
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);
    }
}