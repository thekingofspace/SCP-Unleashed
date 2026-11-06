package net.rai.scp_unleashed.reactor;

import net.rai.scp_unleashed.gas.Gas;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.reactor.core.CoreTower;

import java.util.function.Supplier;

public final class TowerGasOutlet implements GasHandler {
    private final CoreTower Tower;
    private final Supplier<Gas> Kind;
    private final boolean Toxic;

    public TowerGasOutlet(CoreTower Tower, Supplier<Gas> Kind, boolean Toxic) {
        this.Tower = Tower;
        this.Kind = Kind;
        this.Toxic = Toxic;
    }

    private float held() {
        return Toxic ? Tower.Gas : Tower.Steam;
    }

    private void set(float Amount) {
        if (Toxic) {
            Tower.Gas = Math.max(0.0f, Amount);
        } else {
            Tower.Steam = Math.max(0.0f, Amount);
        }
    }

    private int capacity() {
        return Toxic ? Tower.gasCapacity() : Tower.steamCapacity();
    }

    @Override
    public GasStack getGas() {
        return new GasStack(Kind.get(), (int) held());
    }

    @Override
    public int getCapacity() {
        return capacity();
    }

    @Override
    public int fill(GasStack Offer, boolean Simulate) {
        if (Offer.isEmpty() || !Offer.is(Kind.get())) {
            return 0;
        }
        int Accepted = Math.max(0, Math.min(Offer.getAmount(), capacity() - (int) Math.ceil(held())));
        if (Accepted > 0 && !Simulate) {
            set(held() + Accepted);
        }
        return Accepted;
    }

    @Override
    public GasStack drain(int Amount, boolean Simulate) {
        int Taken = Math.min(Amount, (int) held());
        if (Taken <= 0) {
            return GasStack.EMPTY;
        }
        if (!Simulate) {
            set(held() - Taken);
        }
        return new GasStack(Kind.get(), Taken);
    }
}
