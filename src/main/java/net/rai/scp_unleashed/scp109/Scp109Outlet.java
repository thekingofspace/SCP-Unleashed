package net.rai.scp_unleashed.scp109;

import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class Scp109Outlet implements IFluidHandler {
    private final Scp109TapBlockEntity Tap;
    private final boolean Limited;

    public Scp109Outlet(Scp109TapBlockEntity Tap, boolean Limited) {
        this.Tap = Tap;
        this.Limited = Limited;
    }

    public static FluidStack water(int Amount) {
        return new FluidStack(Fluids.WATER, Amount);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int Index) {
        return Tap.hasCanteen() ? water(Scp109TapBlockEntity.ShownAmount) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int Index) {
        return Scp109TapBlockEntity.ShownAmount;
    }

    @Override
    public boolean isFluidValid(int Index, FluidStack Stack) {
        return false;
    }

    @Override
    public int fill(FluidStack Resource, FluidAction Action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack Resource, FluidAction Action) {
        if (Resource.isEmpty() || !FluidStack.isSameFluidSameComponents(Resource, water(1))) {
            return FluidStack.EMPTY;
        }
        return drain(Resource.getAmount(), Action);
    }

    @Override
    public FluidStack drain(int MaxDrain, FluidAction Action) {
        int Amount = Math.min(MaxDrain, Tap.allowance(Limited));
        if (Amount <= 0) {
            return FluidStack.EMPTY;
        }
        if (Action.execute()) {
            Tap.drew(Amount, Limited);
        }
        return water(Amount);
    }
}
