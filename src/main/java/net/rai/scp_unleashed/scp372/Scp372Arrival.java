package net.rai.scp_unleashed.scp372;

import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.archive.AnomalySpawners;

public final class Scp372Arrival {
    private Scp372Arrival() {
    }

    public static boolean arrive(AnomalySpawners.Discovery Found) {
        ServerPlayer Reader = Found.Investigator();
        Scp372Unlock.unlock(Reader);
        if (Scp372Glimpses.blocked(Reader.server, Reader.server.overworld().getGameTime())) {
            return false;
        }
        Scp372Glimpses.send(Reader);
        return true;
    }
}
