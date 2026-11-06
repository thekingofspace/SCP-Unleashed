package net.rai.scp_unleashed.sulfur.tank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public record TankValves(FluidTank Water, FluidTank Acid) implements IFluidHandler {
    public static final int WaterIndex = 0;
    public static final int AcidIndex = 1;

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int Tank) {
        return Tank == WaterIndex ? Water.getFluid() : Tank == AcidIndex ? Acid.getFluid() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int Tank) {
        return Tank == WaterIndex ? Water.getCapacity() : Tank == AcidIndex ? Acid.getCapacity() : 0;
    }

    @Override
    public boolean isFluidValid(int Tank, FluidStack Stack) {
        return Tank == WaterIndex && Water.isFluidValid(Stack);
    }

    @Override
    public int fill(FluidStack Resource, FluidAction Action) {
        return Water.fill(Resource, Action);
    }

    @Override
    public FluidStack drain(FluidStack Resource, FluidAction Action) {
        return Acid.drain(Resource, Action);
    }

    @Override
    public FluidStack drain(int MaxDrain, FluidAction Action) {
        return Acid.drain(MaxDrain, Action);
    }
}
