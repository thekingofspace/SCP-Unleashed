package net.rai.scp_unleashed.terminal;

public final class TerminalChunks {
    public static final int Radius = 2;
    public static final float PositionShare = 0.1f;
    public static final float AreaShare = 0.7f;
    public static final float OwnShare = 0.2f;

    public interface Probe {
        boolean loaded(int X, int Z);
    }

    private TerminalChunks() {
    }

    public static int total(int Size) {
        int Side = Math.max(0, Size) * 2 + 1;
        return Side * Side;
    }

    public static int count(int CentreX, int CentreZ, int Size, Probe Check) {
        int Found = 0;
        int Reach = Math.max(0, Size);
        for (int Dz = -Reach; Dz <= Reach; Dz++) {
            for (int Dx = -Reach; Dx <= Reach; Dx++) {
                if (Check.loaded(CentreX + Dx, CentreZ + Dz)) {
                    Found++;
                }
            }
        }
        return Found;
    }

    public static float fraction(boolean Positioned, int Loaded, int Total, boolean Own) {
        if (!Positioned) {
            return 0.0f;
        }
        float Area = Total <= 0 ? 0.0f : Math.max(0, Math.min(Loaded, Total)) / (float) Total;
        float Value = PositionShare + AreaShare * Area + (Own ? OwnShare : 0.0f);
        return Math.max(0.0f, Math.min(1.0f, Value));
    }
}
