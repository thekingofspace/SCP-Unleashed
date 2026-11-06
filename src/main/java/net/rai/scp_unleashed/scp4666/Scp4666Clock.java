package net.rai.scp_unleashed.scp4666;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public final class Scp4666Clock {
    public static final long Day = 24000L;
    public static final long Midnight = 18000L;
    public static final long Dusk = 12600L;
    public static final long Dawn = 0L;
    public static final long Arrival = 7000L;
    public static final float TicksPerMinute = 1200.0f;
    private static volatile Object Bound;
    private static volatile long Time = Arrival;

    private Scp4666Clock() {
    }

    public static boolean owns(Object Data) {
        return Data != null && Data == Bound;
    }

    public static long serverTime() {
        return Time;
    }

    public static void bind(@Nullable ServerLevel World) {
        Bound = World == null ? null : World.getLevelData();
    }

    public static void set(long Value) {
        Time = Value;
    }

    public static float rate(@Nullable MinecraftServer Server) {
        return Day / (Scp4666Registry.dayMinutes(Server) * TicksPerMinute);
    }

    public static long hourOf(long Value) {
        return Math.floorMod(Value, Day);
    }

    public static boolean night(long Value) {
        long Hour = hourOf(Value);
        return Hour >= Dusk;
    }
}
