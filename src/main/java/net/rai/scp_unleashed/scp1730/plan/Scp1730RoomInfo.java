package net.rai.scp_unleashed.scp1730.plan;

import java.util.List;

public record Scp1730RoomInfo(String Type, int Zone, int MinX, int MinZ, int MaxX, int MaxZ, int Floor, int Height, List<int[]> Doors) {
    public boolean contains(int X, int Y, int Z) {
        return X >= MinX && X <= MaxX && Z >= MinZ && Z <= MaxZ && Y >= Floor && Y <= Floor + Height + 1;
    }
}
