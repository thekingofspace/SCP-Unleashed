package net.rai.scp_unleashed.reactor;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TowerWaterInlet implements IFluidHandler {
    private final Supplier<Float> Held;
    private final Consumer<Float> Store;
    private final int Capacity;

    public TowerWaterInlet(Supplier<Float> Held, Consumer<Float> Store, int Capacity) {
        this.Held = Held;
        this.Store = Store;
        this.Capacity = Capacity;
    }

    public static boolean isWater(FluidStack Stack) {
        return !Stack.isEmpty() && Stack.is(FluidTags.WATER);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int Tank) {
        int Amount = (int) (float) Held.get();
        return Tank == 0 && Amount > 0 ? new FluidStack(Fluids.WATER, Amount) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int Tank) {
        return Tank == 0 ? Capacity : 0;
    }

    @Override
    public boolean isFluidValid(int Tank, FluidStack Stack) {
        return Tank == 0 && isWater(Stack);
    }

    @Override
    public int fill(FluidStack Resource, FluidAction Action) {
        if (!isWater(Resource)) {
            return 0;
        }
        float Current = Held.get();
        int Accepted = Math.max(0, Math.min(Resource.getAmount(), Capacity - (int) Math.ceil(Current)));
        if (Accepted > 0 && Action.execute()) {
            Store.accept(Current + Accepted);
        }
        return Accepted;
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
