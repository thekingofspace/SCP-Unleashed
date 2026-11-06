package net.rai.scp_unleashed.control;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.control.client.ControlClientState;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ControlSessions {
    private static final Set<UUID> Transferring = ConcurrentHashMap.newKeySet();

    private ControlSessions() {
    }

    public static boolean isBoundToSession(Entity Target) {
        if (Target instanceof VacantBody || Transferring.contains(Target.getUUID())) {
            return true;
        }
        return Target instanceof Player User && isControlling(User);
    }

    public static boolean isControlling(Player User) {
        if (User.level().isClientSide) {
            return ControlClientState.isControlled(User.getUUID());
        }
        ControlSession Current = User.getExistingDataOrNull(ControlRegistry.Session.get());
        return Current != null && Current.active();
    }

    public static ControlSession session(Player User) {
        ControlSession Current = User.getExistingDataOrNull(ControlRegistry.Session.get());
        return Current == null ? ControlSession.None : Current;
    }

    public static boolean isTransferring(UUID Id) {
        return Transferring.contains(Id);
    }

    static void beginTransfer(UUID Id) {
        Transferring.add(Id);
    }

    static void endTransfer(UUID Id) {
        Transferring.remove(Id);
    }
}
