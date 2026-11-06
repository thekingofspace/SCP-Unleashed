package net.rai.scp_unleashed.pressure;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public record RestrictedFluidHandler(IFluidHandler Inner, PortRole Role) implements IFluidHandler {
    @Override
    public int getTanks() {
        return Inner.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int Tank) {
        return Inner.getFluidInTank(Tank);
    }

    @Override
    public int getTankCapacity(int Tank) {
        return Inner.getTankCapacity(Tank);
    }

    @Override
    public boolean isFluidValid(int Tank, FluidStack Stack) {
        return Role.canFill() && Inner.isFluidValid(Tank, Stack);
    }

    @Override
    public int fill(FluidStack Resource, FluidAction Action) {
        return Role.canFill() ? Inner.fill(Resource, Action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack Resource, FluidAction Action) {
        return Role.canDrain() ? Inner.drain(Resource, Action) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int MaxDrain, FluidAction Action) {
        return Role.canDrain() ? Inner.drain(MaxDrain, Action) : FluidStack.EMPTY;
    }
}
