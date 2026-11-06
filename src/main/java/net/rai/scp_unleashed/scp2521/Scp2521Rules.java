package net.rai.scp_unleashed.scp2521;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class Scp2521Rules {
    public enum Outcome {
        Ignored,
        Unknown,
        Busy,
        Contained,
        BrokeFree,
        Taken,
        Cooling
    }

    private Scp2521Rules() {
    }

    public static boolean knows(ServerPlayer User) {
        return Scp2521State.get(User.server).knows(User.getUUID());
    }

    public static boolean learn(ServerPlayer User) {
        return Scp2521State.get(User.server).learn(User.getUUID());
    }

    public static Outcome heard(ServerPlayer Speaker, String Raw) {
        if (!Scp2521Name.matches(Raw)) {
            return Outcome.Ignored;
        }
        return spoken(Speaker);
    }

    public static Outcome spoken(ServerPlayer Speaker) {
        if (Speaker.isRemoved() || Speaker.isSpectator() || !knows(Speaker)) {
            return Outcome.Unknown;
        }
        if (Scp2521Trips.active(Speaker.getUUID()) || Scp2521Void.inside(Speaker)) {
            return Outcome.Busy;
        }
        MinecraftServer Server = Speaker.server;
        Scp2521Containment.Anchor Where = Scp2521Containment.newest(Server);
        if (Where != null && Scp2521Containment.contain(Server, Where, Speaker) != null) {
            return Outcome.Contained;
        }
        boolean Broke = Scp2521Containment.breakFree(Server);
        if (Scp2521Trips.start(Speaker)) {
            return Broke ? Outcome.BrokeFree : Outcome.Taken;
        }
        return Broke ? Outcome.BrokeFree : Outcome.Cooling;
    }
}
