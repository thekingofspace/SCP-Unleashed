package net.rai.scp_unleashed.scp323.client;

import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp323.Scp323Bond;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp323ClientBonds {
    private static final Map<Integer, Integer> Stages = new ConcurrentHashMap<>();

    private Scp323ClientBonds() {
    }

    public static void set(int EntityId, int Stage) {
        if (Stage <= 0) {
            Stages.remove(EntityId);
        } else {
            Stages.put(EntityId, Stage);
        }
    }

    public static int stage(@Nullable Entity Target) {
        if (Target == null) {
            return 0;
        }
        Integer Found = Stages.get(Target.getId());
        return Found == null ? 0 : Found;
    }

    public static boolean horned(@Nullable Entity Target) {
        return stage(Target) == Scp323Bond.Horned;
    }

    public static boolean hidden(@Nullable Entity Target) {
        return stage(Target) == Scp323Bond.Transformed;
    }

    public static void clear() {
        Stages.clear();
    }
}
