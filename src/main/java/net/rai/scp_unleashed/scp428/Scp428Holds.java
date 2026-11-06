package net.rai.scp_unleashed.scp428;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp428Holds {
    private static final Map<UUID, UUID> Held = new ConcurrentHashMap<>();

    private Scp428Holds() {
    }

    public static void register(UUID Victim, UUID Core) {
        Held.put(Victim, Core);
    }

    public static void unregister(UUID Victim, UUID Core) {
        Held.remove(Victim, Core);
    }

    public static boolean isHeld(@Nullable Entity Target) {
        return Target != null && Held.containsKey(Target.getUUID());
    }

    @Nullable
    public static UUID coreOf(UUID Victim) {
        return Held.get(Victim);
    }

    @Nullable
    public static Scp428Core holder(ServerLevel World, Entity Victim) {
        UUID Core = Held.get(Victim.getUUID());
        if (Core == null) {
            return null;
        }
        return World.getEntity(Core) instanceof Scp428Core Found && Found.isAlive() ? Found : null;
    }

    public static boolean press(ServerPlayer User) {
        Scp428Core Core = holder(User.serverLevel(), User);
        return Core != null && Core.struggle(User.getUUID());
    }

    public static void clear() {
        Held.clear();
    }
}
