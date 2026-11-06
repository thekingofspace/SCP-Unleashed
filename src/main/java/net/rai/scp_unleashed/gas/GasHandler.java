package net.rai.scp_unleashed.gas;

public interface GasHandler {
    GasStack getGas();

    int getCapacity();

    int fill(GasStack Offer, boolean Simulate);

    GasStack drain(int Amount, boolean Simulate);

    default boolean accepts(Gas Kind) {
        return fill(new GasStack(Kind, 1), true) > 0;
    }

    static GasHandler fillOnly(GasHandler Inner) {
        return new GasHandler() {
            @Override
            public GasStack getGas() {
                return Inner.getGas();
            }

            @Override
            public int getCapacity() {
                return Inner.getCapacity();
            }

            @Override
            public int fill(GasStack Offer, boolean Simulate) {
                return Inner.fill(Offer, Simulate);
            }

            @Override
            public GasStack drain(int Amount, boolean Simulate) {
                return GasStack.EMPTY;
            }
        };
    }

    static GasHandler drainOnly(GasHandler Inner) {
        return new GasHandler() {
            @Override
            public GasStack getGas() {
                return Inner.getGas();
            }

            @Override
            public int getCapacity() {
                return Inner.getCapacity();
            }

            @Override
            public int fill(GasStack Offer, boolean Simulate) {
                return 0;
            }

            @Override
            public GasStack drain(int Amount, boolean Simulate) {
                return Inner.drain(Amount, Simulate);
            }
        };
    }
}
