package net.rai.scp_unleashed.scp902;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class Scp902Privacy {
    private Scp902Privacy() {
    }

    public static boolean hidden(Entity Shown, ServerPlayer Viewer) {
        if (Shown == Viewer || Shown.level() != Viewer.level() || !Scp902Wastes.inside(Viewer) || Scp902Wastes.exempt(Viewer)) {
            return false;
        }
        Scp902State.Trip Mine = Scp902State.get(Viewer.server).trip(Viewer.getUUID());
        if (Mine == null) {
            return false;
        }
        if (Shown instanceof Player) {
            return true;
        }
        return Scp902Wastes.slotOf(Shown.position()) != Mine.Slot;
    }
}
