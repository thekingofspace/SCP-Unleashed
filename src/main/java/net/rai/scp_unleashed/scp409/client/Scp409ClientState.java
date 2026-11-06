package net.rai.scp_unleashed.scp409.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.rai.scp_unleashed.scp409.Scp409Infection;
import net.rai.scp_unleashed.scp409.network.Scp409StatePayload;
import org.jetbrains.annotations.Nullable;

public final class Scp409ClientState {
    private static final Int2ObjectMap<Entry> Entries = new Int2ObjectOpenHashMap<>();

    private Scp409ClientState() {
    }

    public record Entry(int Stage, float Growth, boolean Shattered) {
        public boolean visible() {
            return Stage > 0 && !Shattered;
        }
    }

    public static void receive(Scp409StatePayload Payload) {
        int Id = Payload.EntityId();
        if (Payload.Stage() == Scp409StatePayload.Shattered) {
            Entry Previous = Entries.get(Id);
            Entries.put(Id, new Entry(Previous == null ? Scp409Infection.Stages : Previous.Stage(), 1.0f, true));
            return;
        }
        if (Payload.Stage() <= 0) {
            Entries.remove(Id);
            return;
        }
        Entries.put(Id, new Entry(Payload.Stage(), Payload.Growth() / (float) Scp409Infection.GrowthSteps, false));
    }

    public static void put(int EntityId, int Stage, float Growth) {
        if (Stage <= 0) {
            Entries.remove(EntityId);
        } else {
            Entries.put(EntityId, new Entry(Stage, Growth, false));
        }
    }

    @Nullable
    public static Entry get(int EntityId) {
        return Entries.get(EntityId);
    }

    public static boolean isShattered(int EntityId) {
        Entry Found = Entries.get(EntityId);
        return Found != null && Found.Shattered();
    }

    public static void forget(int EntityId) {
        Entries.remove(EntityId);
    }

    public static void reset() {
        Entries.clear();
    }
}
