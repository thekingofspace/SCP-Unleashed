package net.rai.scp_unleashed.scp006;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public class Scp006VesselHandler implements IFluidHandlerItem {
    private ItemStack Container;

    public Scp006VesselHandler(ItemStack Container) {
        this.Container = Container;
    }

    @Override
    public ItemStack getContainer() {
        return Container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int Tank) {
        int Amount = Container.isEmpty() ? 0 : Scp006VesselItem.amount(Container);
        return Amount <= 0 ? FluidStack.EMPTY : new FluidStack(Scp006Registry.Water.get(), Amount);
    }

    @Override
    public int getTankCapacity(int Tank) {
        return Scp006VesselItem.Capacity;
    }

    @Override
    public boolean isFluidValid(int Tank, FluidStack Stack) {
        return false;
    }

    @Override
    public int fill(FluidStack Resource, FluidAction Action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack Resource, FluidAction Action) {
        if (Resource.isEmpty() || !Resource.getFluid().isSame(Scp006Registry.Water.get())) {
            return FluidStack.EMPTY;
        }
        return drain(Resource.getAmount(), Action);
    }

    @Override
    public FluidStack drain(int MaxDrain, FluidAction Action) {
        if (Container.isEmpty() || Container.getCount() != 1 || MaxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        int Amount = Scp006VesselItem.amount(Container);
        if (Amount <= 0) {
            return FluidStack.EMPTY;
        }
        int Drained = Math.min(Amount, MaxDrain);
        if (Action.execute() && Scp006VesselItem.take(Container, Drained)) {
            Container = ItemStack.EMPTY;
        }
        return new FluidStack(Scp006Registry.Water.get(), Drained);
    }
}
