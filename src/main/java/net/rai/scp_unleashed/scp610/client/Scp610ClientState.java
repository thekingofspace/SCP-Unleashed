package net.rai.scp_unleashed.scp610.client;

import java.util.HashMap;
import java.util.Map;

public final class Scp610ClientState {
    private static final Map<Integer, Integer> Stages = new HashMap<>();

    private Scp610ClientState() {
    }

    public static void set(int EntityId, int Stage) {
        if (Stage <= 0) {
            Stages.remove(EntityId);
        } else {
            Stages.put(EntityId, Stage);
        }
    }

    public static int stage(int EntityId) {
        return Stages.getOrDefault(EntityId, 0);
    }

    public static void forget(int EntityId) {
        Stages.remove(EntityId);
    }

    public static void reset() {
        Stages.clear();
    }
}
