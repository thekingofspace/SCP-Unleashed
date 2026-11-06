package net.rai.scp_unleashed.scp1730.plan;

public record Scp1730Basin(String Kind, int MinX, int MinZ, int MaxX, int MaxZ, int TopY, int BottomY, int Zone) {
    public boolean contains(int X, int Y, int Z) {
        return X >= MinX && X <= MaxX && Z >= MinZ && Z <= MaxZ && Y >= BottomY && Y <= TopY;
    }

    public boolean wet() {
        return Kind.equals("water");
    }
}
