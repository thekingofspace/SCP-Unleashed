package net.rai.scp_unleashed.mining;

public final class MinerRules {
    public static final int DrawPerMiner = 10;
    public static final int TicksPerPoint = 400;
    public static final int BufferSize = 16;
    public static final int TankCapacity = 1000;
    public static final int WaterPerSecond = 2;
    public static final int MaxHeat = 100;
    public static final int DryHeatPerSecond = 20;
    public static final int CoolPerSecond = 10;
    public static final int WorkScale = 1000;
    public static final int SecondTicks = 20;
    public static final int TicksPerMinute = 1200;

    private MinerRules() {
    }

    public static int demand(int Miners) {
        return Math.max(0, Miners) * DrawPerMiner;
    }

    public static double share(long Received, int Demand) {
        if (Demand <= 0 || Received <= 0L) {
            return 0.0;
        }
        return Math.min(1.0, (double) Received / Demand);
    }

    public static double pointsPerMinute(double Share) {
        return Math.max(0.0, Math.min(1.0, Share)) * TicksPerMinute / TicksPerPoint;
    }

    public static int waterPerMinute() {
        return WaterPerSecond * TicksPerMinute / SecondTicks;
    }

    public static boolean isSecond(long Now, long Phase) {
        return Math.floorMod(Now + Phase, (long) SecondTicks) == 0L;
    }

    public interface Coolant {
        int available();

        int drain(int Amount);
    }

    public static final class Engine {
        private int Heat;
        private boolean Overheated;
        private int Work;
        private int Buffer;
        private double Share;

        public void step(double NewShare, boolean Second, Coolant Water) {
            Share = Math.max(0.0, Math.min(1.0, NewShare));
            if (Second) {
                if (isMining()) {
                    boolean Cooled = Water.available() >= WaterPerSecond && Water.drain(WaterPerSecond) >= WaterPerSecond;
                    Heat = Cooled ? Math.max(0, Heat - CoolPerSecond) : Math.min(MaxHeat, Heat + DryHeatPerSecond);
                } else {
                    Heat = Math.max(0, Heat - CoolPerSecond);
                }
                if (Heat >= MaxHeat) {
                    Overheated = true;
                } else if (Overheated && Heat == 0 && Water.available() >= WaterPerSecond) {
                    Overheated = false;
                }
            }
            if (!isMining()) {
                return;
            }
            Work += (int) Math.round(Share * WorkScale);
            int Cost = TicksPerPoint * WorkScale;
            while (Work >= Cost && Buffer < BufferSize) {
                Work -= Cost;
                Buffer++;
            }
            if (Buffer >= BufferSize) {
                Work = Math.min(Work, Cost - 1);
            }
        }

        public boolean isPowered() {
            return Share > 0.0;
        }

        public boolean isMining() {
            return Share > 0.0 && !Overheated && Buffer < BufferSize;
        }

        public boolean isOverheated() {
            return Overheated;
        }

        public boolean isFull() {
            return Buffer >= BufferSize;
        }

        public int getHeat() {
            return Heat;
        }

        public int getBuffer() {
            return Buffer;
        }

        public int getWork() {
            return Work;
        }

        public double getShare() {
            return Share;
        }

        public int takeBuffer() {
            int Taken = Buffer;
            Buffer = 0;
            return Taken;
        }

        public void restore(int SavedHeat, boolean SavedOverheated, int SavedWork, int SavedBuffer) {
            Heat = Math.max(0, Math.min(MaxHeat, SavedHeat));
            Overheated = SavedOverheated;
            Work = Math.max(0, Math.min(TicksPerPoint * WorkScale - 1, SavedWork));
            Buffer = Math.max(0, Math.min(BufferSize, SavedBuffer));
        }
    }
}
