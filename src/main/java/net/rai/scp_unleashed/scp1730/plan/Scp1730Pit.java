package net.rai.scp_unleashed.scp1730.plan;

public record Scp1730Pit(int MinX, int MinZ, int MaxX, int MaxZ, int RimY, int BottomY, int Zone) {
    public boolean lethal(int X, int Y, int Z) {
        return X >= MinX && X <= MaxX && Z >= MinZ && Z <= MaxZ && Y < RimY - 3 && Y >= BottomY - 2;
    }
}
