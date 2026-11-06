package net.rai.scp_unleashed.scp1730.rooms;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Iterator;

public final class Scp1730EchoTrack {
    public static final int Delay = 50;
    public static final int MaxFrames = 200;
    private final ArrayDeque<Frame> Frames = new ArrayDeque<>();

    public record Frame(long Tick, double X, double Y, double Z, float Yaw, float HeadYaw, float Pitch, float Walk, float WalkSpeed, boolean Crouching, boolean Swinging) {
    }

    public void record(Frame Made) {
        Frame Newest = Frames.peekLast();
        if (Newest != null && Made.Tick() <= Newest.Tick()) {
            Frames.clear();
        }
        Frames.addLast(Made);
        while (Frames.size() > MaxFrames) {
            Frames.removeFirst();
        }
    }

    @Nullable
    public Frame replay(long Now) {
        long Wanted = Now - Delay;
        while (!Frames.isEmpty() && Frames.peekFirst().Tick() < Wanted - 1) {
            Frames.removeFirst();
        }
        Iterator<Frame> Walk = Frames.iterator();
        while (Walk.hasNext()) {
            Frame Each = Walk.next();
            if (Each.Tick() == Wanted) {
                return Each;
            }
            if (Each.Tick() > Wanted) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    public Frame before(long Now) {
        long Wanted = Now - Delay - 1;
        for (Frame Each : Frames) {
            if (Each.Tick() == Wanted) {
                return Each;
            }
        }
        return null;
    }

    public int size() {
        return Frames.size();
    }

    public boolean idle(long Now) {
        Frame Newest = Frames.peekLast();
        return Newest == null || Newest.Tick() < Now - Delay - 1;
    }

    public void clear() {
        Frames.clear();
    }
}
