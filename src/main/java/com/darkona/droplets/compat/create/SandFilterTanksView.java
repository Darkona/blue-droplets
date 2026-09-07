package com.darkona.droplets.compat.create;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * The Sand Filter's fluid handler without a side (Jade, other inspectors): both tanks, dirty first, read only. Water
 * only goes in at the back and out at the front, so nothing is filled or drained through here.
 */
final class SandFilterTanksView implements IFluidHandler
{
    private final SandFilterBlockEntity filter;

    SandFilterTanksView(SandFilterBlockEntity filter)
    {
        this.filter = filter;
    }

    private IFluidHandler tank(int tank)
    {
        return (tank == 0 ? filter.dirtyTank : filter.purifiedTank).getPrimaryHandler();
    }

    @Override
    public int getTanks()
    {
        return 2;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank)
    {
        return tank == 0 || tank == 1 ? tank(tank).getFluidInTank(0) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank)
    {
        return tank == 0 || tank == 1 ? tank(tank).getTankCapacity(0) : 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack)
    {
        return false;
    }

    @Override
    public int fill(@NotNull FluidStack resource, @NotNull FluidAction action)
    {
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action)
    {
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action)
    {
        return FluidStack.EMPTY;
    }
}
