package net.rai.scp_unleashed.raid.base;

public final class BaseWeights {
    public static final int SampleEvery = 200;
    public static final double Presence = 0.6;
    public static final double Settled = 0.8;
    public static final double SettledRange = 6.0;
    public static final double RespawnPresence = 0.35;
    public static final double Sleep = 18.0;
    public static final double SpawnSet = 12.0;
    public static final double Placed = 0.12;
    public static final double ModBlock = 0.4;
    public static final double Tile = 1.6;
    public static final double Bed = 6.0;
    public static final double Storage = 2.0;
    public static final double Containment = 1.5;

    private BaseWeights() {
    }

    public static double presence(double Moved) {
        return Moved < SettledRange ? Presence + Settled : Presence;
    }

    public static double placed(boolean Mod, boolean HasTile, boolean IsBed, boolean IsStorage, boolean IsContainment) {
        double Amount = Placed;
        if (Mod) {
            Amount += ModBlock;
        }
        if (HasTile) {
            Amount += Tile;
        }
        if (IsBed) {
            Amount += Bed;
        }
        if (IsStorage) {
            Amount += Storage;
        }
        if (IsContainment) {
            Amount += Containment;
        }
        return Amount;
    }
}
