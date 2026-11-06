package net.rai.scp_unleashed.control;

import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.scp012.Scp012Exposure;
import net.rai.scp_unleashed.scp016.Scp016Affliction;
import net.rai.scp_unleashed.scp020.Scp020Affliction;

public final class ControlResync {
    private ControlResync() {
    }

    public static void afflictions(ServerPlayer User) {
        if (User.connection == null) {
            return;
        }
        if (Scp016Affliction.isAfflicted(User)) {
            int Elapsed = Scp016Affliction.elapsed(User);
            Scp016Affliction.sync(User, Scp016Affliction.stageOf(Elapsed), Scp016Affliction.progressOf(Elapsed));
        } else {
            Scp016Affliction.sync(User, 0, 0.0f);
        }
        Scp020Affliction.sync(User);
        Scp012Exposure.resend(User);
    }
}
