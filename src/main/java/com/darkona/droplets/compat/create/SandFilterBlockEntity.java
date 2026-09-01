package com.darkona.droplets.compat.create;

import com.darkona.droplets.foundation.config.CompatConfig;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.utility.CreateLang;
import com.darkona.droplets.content.purity.WaterPurity;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SandFilterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation
{
    public static final int TANK_SIZE = 1000;
    SmartFluidTankBehaviour dirtyTank;
    SmartFluidTankBehaviour purifiedTank;
    /** The block in front, recomputed only when the filter is turned. */
    private Direction nextFacing;
    private BlockPos nextPos;
    /**
     * Watches the block in front: NeoForge invalidates it when a block entity is placed, removed, loaded or unloaded
     * there, and only then is the filter in front looked up again. Its capability is not used.
     */
    private @Nullable BlockCapabilityCache<IFluidHandler, @Nullable Direction> nextWatch;
    private @Nullable SandFilterBlockEntity next;
    private boolean nextStale = true;
    private final SandFilterTanksView tanksView = new SandFilterTanksView(this);

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
     * Back: the dirty tank; front: the purified tank; no side: both, read only, for Jade and other inspectors.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CreateRegistry.SAND_FILTER_BE.get(),
                (be, side) -> {
                    if (side == null)
                        return be.tanksView;
                    Direction facing = be.getBlockState().getValue(SandFilterBlock.FACING);
                    if (side == facing)
                        return be.purifiedTank.getCapability();
                    return side == facing.getOpposite() ? be.dirtyTank.getCapability() : null;
                }
        );
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

        if(water.is(FluidTags.WATER))
            WaterPurity.addPurity(water, filteredPurity(WaterPurity.getPurity(water)));

        int accepted = purified.fill(water, IFluidHandler.FluidAction.SIMULATE);
        if(accepted <= 0)
            return;

        int drained = dirty.drain(accepted, IFluidHandler.FluidAction.EXECUTE).getAmount();
        purified.fill(water.copyWithAmount(drained), IFluidHandler.FluidAction.EXECUTE);
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
        SandFilterBlockEntity ahead = nextFilter(getBlockState().getValue(SandFilterBlock.FACING));
        if(ahead == null)
            return;
        SmartFluidTank nextDirty = ahead.dirtyTank.getPrimaryHandler();
        int accepted = nextDirty.fill(purified.drain(rate, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.SIMULATE);
        if(accepted > 0)
            nextDirty.fill(purified.drain(accepted, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
    }

    /**
     * The filter right in front, facing the same way, or null. The block entity there is looked up only after the
     * filter is turned or the watch on the block in front is invalidated, not every tick.
     */
    private @Nullable SandFilterBlockEntity nextFilter(Direction facing)
    {
        if(facing != nextFacing)
        {
            nextFacing = facing;
            nextPos = worldPosition.relative(facing);
            nextWatch = BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, (ServerLevel) level, nextPos, facing.getOpposite(), () -> !isRemoved(), () -> nextStale = true);
            nextStale = true;
        }
        if(nextStale || next != null && next.isRemoved())
        {
            nextStale = false;
            // Only a query arms the watch again after an invalidation.
            nextWatch.getCapability();
            next = level.getBlockEntity(nextPos) instanceof SandFilterBlockEntity filter ? filter : null;
        }
        return next != null && next.getBlockState().getValue(SandFilterBlock.FACING) == facing ? next : null;
    }

    public boolean hasFluid()
    {
        return !dirtyTank.isEmpty() || !purifiedTank.isEmpty();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking)
    {
            LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");
            CreateLang.translate("gui.goggles.fluid_container")
                    .forGoggles(tooltip);

            int dirtyWaterAmount = dirtyTank.getPrimaryHandler().getFluidAmount();
            int purifiedWaterAmount = purifiedTank.getPrimaryHandler().getFluidAmount();

        buildTooltip(tooltip, mb, dirtyWaterAmount, dirtyTank);

        buildTooltip(tooltip, mb, purifiedWaterAmount, purifiedTank);

        if(dirtyTank.isEmpty() && purifiedTank.isEmpty()){
            CreateLang.translate("gui.goggles.fluid_container.capacity")
                    .add(CreateLang.number(dirtyTank.getPrimaryHandler().getTankCapacity(0))
                            .add(mb)
                            .style(ChatFormatting.GOLD))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
        }

        return hasFluid();
    }

    private void buildTooltip(List<Component> tooltip, LangBuilder mb, int purifiedWaterAmount, SmartFluidTankBehaviour purifiedTank) {
        if(!purifiedTank.isEmpty())
        {
            CreateLang.builder()
                    .text(WaterPurity.enabled() ? WaterPurity.getPurityText(WaterPurity.getPurity(purifiedTank.getPrimaryHandler().getFluid())) + " " : "")
                    .add(CreateLang.fluidName(purifiedTank.getPrimaryHandler().getFluid()))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);

            CreateLang.builder()
                    .add(CreateLang.number(purifiedWaterAmount)
                            .add(mb)
                            .style(ChatFormatting.GOLD))
                    .text(ChatFormatting.GRAY, " / ")
                    .add(CreateLang.number(purifiedTank.getPrimaryHandler().getCapacity())
                            .add(mb)
                            .style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);
        }
    }
}