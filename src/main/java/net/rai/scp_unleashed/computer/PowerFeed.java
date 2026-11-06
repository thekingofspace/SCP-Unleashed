package net.rai.scp_unleashed.computer;

public final class PowerFeed {
    public static final int Grace = 20;
    public static final int LoadGrace = 40;

    private int Received;
    private long LastFed = Long.MIN_VALUE / 2;
    private long FedSince = -1;
    private boolean Running;
    private boolean Loaded;

    public void insert(int Amount) {
        Received += Amount;
    }

    public boolean isRunning() {
        return Running;
    }

    public void restore(boolean WasRunning) {
        Running = WasRunning;
    }

    public boolean tick(long Now, int Demand, int BootDelay) {
        if (!Loaded) {
            Loaded = true;
            if (Running) {
                LastFed = Now + LoadGrace;
            }
        }
        boolean Fed = Demand <= 0 || Received >= Demand;
        Received = 0;
        if (Fed) {
            if (FedSince < 0) {
                FedSince = Now;
            }
            LastFed = Math.max(LastFed, Now);
        } else {
            FedSince = -1;
        }
        boolean Lit = Running ? Now - LastFed <= Grace : FedSince >= 0 && Now - FedSince >= BootDelay;
        if (Lit == Running) {
            return false;
        }
        Running = Lit;
        return true;
    }
}
