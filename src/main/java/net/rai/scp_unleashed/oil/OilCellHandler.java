package net.rai.scp_unleashed.oil;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public record OilCellHandler(@Nullable FluidTank Fill, @Nullable FluidTank Drain) implements IFluidHandler {
    private FluidTank shown(int Tank) {
        if (Fill != null && Drain != null) {
            return Tank == 0 ? Fill : Drain;
        }
        return Fill != null ? Fill : Drain;
    }

    @Override
    public int getTanks() {
        return Fill != null && Drain != null && Fill != Drain ? 2 : 1;
    }

    @Override
    public FluidStack getFluidInTank(int Tank) {
        FluidTank Shown = shown(Tank);
        return Shown == null ? FluidStack.EMPTY : Shown.getFluid();
    }

    @Override
    public int getTankCapacity(int Tank) {
        FluidTank Shown = shown(Tank);
        return Shown == null ? 0 : Shown.getCapacity();
    }

    @Override
    public boolean isFluidValid(int Tank, FluidStack Stack) {
        return Fill != null && shown(Tank) == Fill && Fill.isFluidValid(Stack);
    }

    @Override
    public int fill(FluidStack Resource, FluidAction Action) {
        return Fill == null ? 0 : Fill.fill(Resource, Action);
    }

    @Override
    public FluidStack drain(FluidStack Resource, FluidAction Action) {
        return Drain == null ? FluidStack.EMPTY : Drain.drain(Resource, Action);
    }

    @Override
    public FluidStack drain(int MaxDrain, FluidAction Action) {
        return Drain == null ? FluidStack.EMPTY : Drain.drain(MaxDrain, Action);
    }
}
