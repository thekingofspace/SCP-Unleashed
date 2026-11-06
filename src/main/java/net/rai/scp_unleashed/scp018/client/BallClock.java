package net.rai.scp_unleashed.scp018.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public final class BallClock {
    public static final double JumpThreshold = 0.6;
    public static final double LargestOffset = 3.0;
    public static final double StaleSeconds = 0.25;
    public static final double SettleRate = 1.0;
    public static final double DisplayDelay = 1.0;
    private static Level Bound;
    private static double LastRaw = Double.NaN;
    private static long LastNanos;
    private static double Offset;

    private BallClock() {
    }

    public static double frame(Level World, float PartialTick) {
        double Raw = World.getGameTime() + PartialTick;
        long Nanos = System.nanoTime();
        if (Bound != World || Double.isNaN(LastRaw)) {
            Bound = World;
            LastRaw = Raw;
            LastNanos = Nanos;
            Offset = 0.0;
            return Raw - DisplayDelay;
        }
        double Seconds = Math.max(0.0, (Nanos - LastNanos) / 1.0e9);
        if (Raw == LastRaw && Seconds < 0.002) {
            return Raw + Offset - DisplayDelay;
        }
        Minecraft Client = Minecraft.getInstance();
        boolean Stopped = Client.isPaused() || World.tickRateManager().isFrozen();
        double Expected = Stopped ? 0.0 : Seconds * World.tickRateManager().tickrate();
        double Jump = Raw - LastRaw - Expected;
        if (Seconds > StaleSeconds) {
            Offset = 0.0;
        } else if (Math.abs(Jump) > JumpThreshold) {
            Offset -= Math.rint(Jump);
        }
        if (Math.abs(Offset) > LargestOffset) {
            Offset = 0.0;
        }
        double Settle = Math.min(Math.abs(Offset), Seconds * SettleRate);
        Offset -= Math.signum(Offset) * Settle;
        LastRaw = Raw;
        LastNanos = Nanos;
        return Raw + Offset - DisplayDelay;
    }

    public static double tick(Level World) {
        double Raw = World.getGameTime();
        if (Bound != World || Double.isNaN(LastRaw)) {
            return Raw;
        }
        return Raw + Offset;
    }

    public static void reset() {
        Bound = null;
        LastRaw = Double.NaN;
        Offset = 0.0;
    }
}
