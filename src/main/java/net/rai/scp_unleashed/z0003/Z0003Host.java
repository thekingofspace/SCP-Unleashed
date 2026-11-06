package net.rai.scp_unleashed.z0003;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class Z0003Host {
    @Nullable
    private static Boolean Forced;

    private Z0003Host() {
    }

    public static boolean isLocal(MinecraftServer Server) {
        if (Forced != null) {
            return Forced;
        }
        return !Server.isDedicatedServer() && !(Server instanceof GameTestServer);
    }

    public static void force(@Nullable Boolean Local) {
        Forced = Local;
    }
}
