package net.rai.scp_unleashed.scp087;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Scp087Visit {
    public final UUID Visitor;
    public Scp087Instance Instance;
    public int Flights;
    public int Synced = -1;
    public int Deepest;
    public long NextFace;
    public long NextAmbience;
    public long NextWhisper;
    @Nullable
    public UUID Face;

    public Scp087Visit(UUID Visitor, Scp087Instance Instance) {
        this.Visitor = Visitor;
        this.Instance = Instance;
    }
}
