package net.rai.scp_unleashed.scp087;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Scp087Instance {
    public final int Slot;
    public final UUID Token = UUID.randomUUID();
    public final List<UUID> Members = new ArrayList<>();
    public int Wraps;
    @Nullable
    public UUID Hunter;
    public long HunterReady;
    public boolean Ending;

    public Scp087Instance(int Slot) {
        this.Slot = Slot;
    }

    public int originX() {
        return Scp087Stairwell.originX(Slot);
    }
}
