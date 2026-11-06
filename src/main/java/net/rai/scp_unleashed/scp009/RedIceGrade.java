package net.rai.scp_unleashed.scp009;

public enum RedIceGrade {
    Shard(64, 6, 4),
    Fragment(16, 3, 2);

    private final int MaxBlocks;
    private final int Radius;
    private final int PerTick;

    RedIceGrade(int MaxBlocks, int Radius, int PerTick) {
        this.MaxBlocks = MaxBlocks;
        this.Radius = Radius;
        this.PerTick = PerTick;
    }

    public int getMaxBlocks() {
        return MaxBlocks;
    }

    public int getRadius() {
        return Radius;
    }

    public int getPerTick() {
        return PerTick;
    }

    public boolean isUnique() {
        return this == Shard;
    }
}
