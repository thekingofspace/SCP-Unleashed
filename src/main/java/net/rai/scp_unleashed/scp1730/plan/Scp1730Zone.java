package net.rai.scp_unleashed.scp1730.plan;

public record Scp1730Zone(int Index, String Kind, int MinX, int MinY, int MinZ, int MaxX, int MaxY, int MaxZ, Scp1730Spot Entry, String Objective) {
    public boolean contains(int X, int Y, int Z) {
        return X >= MinX && X <= MaxX && Y >= MinY && Y <= MaxY && Z >= MinZ && Z <= MaxZ;
    }
}
