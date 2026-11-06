package net.rai.scp_unleashed.forcefield.client;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ForcefieldRipples {
    public static final int MaxRipples = 48;
    public static final int FrameTicks = 2;
    public static final int Frames = 8;
    public static final int LifeTicks = FrameTicks * Frames;
    private static final Deque<Ripple> Active = new ArrayDeque<>();

    private ForcefieldRipples() {
    }

    public static void add(Vec3 Point, boolean AlongX, float Strength, long Now) {
        if (Active.size() >= MaxRipples) {
            Active.pollFirst();
        }
        double Snap = 16.0;
        Vec3 Snapped = new Vec3(Math.round(Point.x * Snap) / Snap, Math.round(Point.y * Snap) / Snap, Math.round(Point.z * Snap) / Snap);
        Active.addLast(new Ripple(Snapped, AlongX, Strength, Now));
    }

    public static Iterable<Ripple> active(long Now) {
        while (!Active.isEmpty() && Now - Active.peekFirst().Start() >= LifeTicks) {
            Active.pollFirst();
        }
        return Active;
    }

    public static void clear() {
        Active.clear();
    }

    public record Ripple(Vec3 Point, boolean AlongX, float Strength, long Start) {
    }
}
