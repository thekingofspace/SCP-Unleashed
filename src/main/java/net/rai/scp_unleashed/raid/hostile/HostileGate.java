package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.server.MinecraftServer;
import net.rai.scp_unleashed.archive.investigation.Investigations;

public final class HostileGate {
    public static final int DocumentsNeeded = 5;

    private HostileGate() {
    }

    public static int opened(MinecraftServer Server) {
        return Investigations.get(Server).opened(Server);
    }

    public static boolean unlocked(MinecraftServer Server) {
        return opened(Server) >= DocumentsNeeded;
    }
}
