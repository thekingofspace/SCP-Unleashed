package net.rai.scp_unleashed.pressure;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public enum FluidVoid implements IFluidHandler {
    Instance;

    public static final int ShownCapacity = 1_000_000;

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int Tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int Tank) {
        return ShownCapacity;
    }

    @Override
    public boolean isFluidValid(int Tank, FluidStack Stack) {
        return true;
    }

    @Override
    public int fill(FluidStack Resource, FluidAction Action) {
        return Resource.isEmpty() ? 0 : Resource.getAmount();
    }

    @Override
    public FluidStack drain(FluidStack Resource, FluidAction Action) {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int MaxDrain, FluidAction Action) {
        return FluidStack.EMPTY;
    }
}
