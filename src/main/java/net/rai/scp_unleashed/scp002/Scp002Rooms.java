package net.rai.scp_unleashed.scp002;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp002Rooms {
    public static final long Memory = 10L;
    private static final Map<UUID, Long> Occupied = new ConcurrentHashMap<>();

    private Scp002Rooms() {
    }

    public static void mark(Entity Occupant, long Now) {
        Occupied.put(Occupant.getUUID(), Now);
    }

    public static boolean isInside(Entity Candidate, long Now) {
        Long Seen = Occupied.get(Candidate.getUUID());
        if (Seen == null) {
            return false;
        }
        if (Now - Seen > Memory || Seen > Now) {
            Occupied.remove(Candidate.getUUID());
            return false;
        }
        return true;
    }

    public static void forgetAll() {
        Occupied.clear();
    }
}
