package net.rai.scp_unleashed.gas;

import net.minecraft.nbt.CompoundTag;

import java.util.function.Predicate;

public class GasTank implements GasHandler {
    private final int Capacity;
    private final Predicate<Gas> Filter;
    private final Runnable Changed;
    private GasStack Contents = GasStack.EMPTY;

    public GasTank(int Capacity, Predicate<Gas> Filter, Runnable Changed) {
        this.Capacity = Capacity;
        this.Filter = Filter;
        this.Changed = Changed;
    }

    @Override
    public GasStack getGas() {
        return Contents;
    }

    @Override
    public int getCapacity() {
        return Capacity;
    }

    public int getAmount() {
        return Contents.getAmount();
    }

    public int getSpace() {
        return Capacity - Contents.getAmount();
    }

    public boolean isEmpty() {
        return Contents.isEmpty();
    }

    public boolean isValid(Gas Kind) {
        return Kind != null && Filter.test(Kind);
    }

    @Override
    public int fill(GasStack Offer, boolean Simulate) {
        if (Offer.isEmpty() || !isValid(Offer.getGas()) || (!Contents.isEmpty() && !Contents.isSameGas(Offer))) {
            return 0;
        }
        int Accepted = Math.min(Offer.getAmount(), getSpace());
        if (Accepted > 0 && !Simulate) {
            Contents = Contents.isEmpty() ? Offer.copyWithAmount(Accepted) : Contents.grow(Accepted);
            Changed.run();
        }
        return Math.max(0, Accepted);
    }

    @Override
    public GasStack drain(int Amount, boolean Simulate) {
        if (Contents.isEmpty() || Amount <= 0) {
            return GasStack.EMPTY;
        }
        int Taken = Math.min(Amount, Contents.getAmount());
        GasStack Result = Contents.copyWithAmount(Taken);
        if (!Simulate) {
            Contents = Contents.shrink(Taken);
            Changed.run();
        }
        return Result;
    }

    public void setGas(GasStack Stack) {
        Contents = Stack.isEmpty() ? GasStack.EMPTY : Stack.copyWithAmount(Math.min(Capacity, Stack.getAmount()));
        Changed.run();
    }

    public void save(CompoundTag Tag, String Key) {
        Tag.put(Key, Contents.save());
    }

    public void load(CompoundTag Tag, String Key) {
        GasStack Loaded = GasStack.load(Tag.getCompound(Key));
        Contents = Loaded.isEmpty() || !isValid(Loaded.getGas()) ? GasStack.EMPTY : Loaded.copyWithAmount(Math.min(Capacity, Loaded.getAmount()));
    }
}
