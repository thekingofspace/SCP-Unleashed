package net.rai.scp_unleashed.scp217.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp217.Scp217Clockwork;
import net.rai.scp_unleashed.scp217.network.Scp217StatePayload;
import org.jetbrains.annotations.Nullable;

public final class Scp217ClientState {
    public static final float[] Rates = {0.0f, 0.0f, 0.5f, 1.0f, 0.8f};
    public static final float WindingEndRate = 0.08f;
    private static final Int2ObjectMap<Entry> Entries = new Int2ObjectOpenHashMap<>();

    private Scp217ClientState() {
    }

    public static final class Entry {
        private int Stage;
        private boolean Perfect;
        private boolean Winding;
        private int WindingTicks;
        private float Steps;
        private float LastSteps;

        public int stage() {
            return Stage;
        }

        public boolean perfect() {
            return Perfect;
        }

        public boolean winding() {
            return Winding;
        }

        public float windingProgress() {
            return Winding ? Mth.clamp(WindingTicks / (float) Scp217Clockwork.WindingTicks, 0.0f, 1.0f) : 0.0f;
        }

        public float steps(float Partial) {
            return Mth.lerp(Partial, LastSteps, Steps);
        }

        private float rate() {
            float Base = Rates[Mth.clamp(Stage, 0, Rates.length - 1)];
            return Winding ? Mth.lerp(windingProgress(), Base, WindingEndRate) : Base;
        }

        private void tick() {
            LastSteps = Steps;
            if (Winding) {
                WindingTicks++;
            }
            Steps += rate() / 20.0f;
        }
    }

    public static void receive(Scp217StatePayload Payload) {
        if (Payload.State() == 0) {
            Entries.remove(Payload.EntityId());
            return;
        }
        Entry Found = Entries.computeIfAbsent(Payload.EntityId(), Id -> new Entry());
        if (Payload.winding() && !Found.Winding) {
            Found.WindingTicks = 0;
        }
        Found.Stage = Payload.stage();
        Found.Perfect = Payload.perfect();
        Found.Winding = Payload.winding();
    }

    @Nullable
    public static Entry get(int EntityId) {
        return Entries.get(EntityId);
    }

    public static int stage(int EntityId) {
        Entry Found = Entries.get(EntityId);
        return Found == null ? 0 : Found.Stage;
    }

    public static void tick() {
        for (Entry Found : Entries.values()) {
            Found.tick();
        }
    }

    public static void forget(int EntityId) {
        Entries.remove(EntityId);
    }

    public static void reset() {
        Entries.clear();
    }
}
