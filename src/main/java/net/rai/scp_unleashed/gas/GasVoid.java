package net.rai.scp_unleashed.gas;

public enum GasVoid implements GasHandler {
    Instance;

    @Override
    public GasStack getGas() {
        return GasStack.EMPTY;
    }

    @Override
    public int getCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int fill(GasStack Offer, boolean Simulate) {
        return Offer.isEmpty() ? 0 : Offer.getAmount();
    }

    @Override
    public GasStack drain(int Amount, boolean Simulate) {
        return GasStack.EMPTY;
    }
}
