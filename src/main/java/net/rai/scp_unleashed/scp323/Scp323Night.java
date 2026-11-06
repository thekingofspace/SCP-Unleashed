package net.rai.scp_unleashed.scp323;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public final class Scp323Night {
    public static final long DayLength = 24000L;
    public static final long Nightfall = 13000L;
    public static final long Dawn = 23000L;

    private Scp323Night() {
    }

    public static long time(MinecraftServer Server) {
        return Server.overworld().getDayTime();
    }

    public static long time(Level World) {
        MinecraftServer Server = World.getServer();
        return Server == null ? World.getDayTime() : time(Server);
    }

    public static boolean isNight(long Time) {
        long Hour = Math.floorMod(Time, DayLength);
        return Hour >= Nightfall && Hour < Dawn;
    }

    public static long nightId(long Time) {
        return Math.floorDiv(Time - Nightfall, DayLength);
    }

    public static boolean isNight(Level World) {
        return isNight(time(World));
    }

    public static long nightId(Level World) {
        return nightId(time(World));
    }

    public static long currentNight(Level World) {
        long Time = time(World);
        return isNight(Time) ? nightId(Time) : -1L;
    }
}
