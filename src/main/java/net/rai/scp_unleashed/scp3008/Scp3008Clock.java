package net.rai.scp_unleashed.scp3008;

import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public final class Scp3008Clock {
    private static volatile Object Bound;
    private static volatile long Time = Scp3008Hours.Arrival;

    private Scp3008Clock() {
    }

    public static boolean owns(Object Data) {
        return Data != null && Data == Bound;
    }

    public static boolean bound() {
        return Bound != null;
    }

    public static long time() {
        return Time;
    }

    public static void bind(@Nullable ServerLevel World) {
        Bound = World == null ? null : World.getLevelData();
    }

    public static void set(long Value) {
        Time = Value;
    }
}
