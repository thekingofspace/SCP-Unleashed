package net.rai.scp_unleashed.scp018;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BallPath {
    public static final double StartSlack = 1.0e-9;
    public static final double KeepBehind = 6.0;
    public static final double CutSlack = 1.0e-4;
    private final List<BallSegment> Segments = new ArrayList<>();

    public boolean isEmpty() {
        return Segments.isEmpty();
    }

    public int size() {
        return Segments.size();
    }

    public List<BallSegment> segments() {
        return List.copyOf(Segments);
    }

    public BallSegment first() {
        return Segments.get(0);
    }

    public BallSegment last() {
        return Segments.get(Segments.size() - 1);
    }

    public void removeFirst() {
        Segments.remove(0);
    }

    public void clear() {
        Segments.clear();
    }

    public void add(BallSegment Segment) {
        Segments.add(Segment);
    }

    public void merge(List<BallSegment> Incoming) {
        if (Incoming.isEmpty()) {
            return;
        }
        double From = Incoming.get(0).StartTime() - StartSlack;
        Segments.removeIf(Segment -> Segment.StartTime() >= From);
        Segments.addAll(Incoming);
    }

    public void shift(double Offset) {
        Segments.replaceAll(Segment -> Segment.shifted(Offset));
    }

    public void prune(double Time) {
        while (Segments.size() > 1 && Segments.get(1).StartTime() <= Time - KeepBehind) {
            Segments.remove(0);
        }
    }

    @Nullable
    public BallSegment at(double Time) {
        if (Segments.isEmpty()) {
            return null;
        }
        for (int Index = Segments.size() - 1; Index >= 0; Index--) {
            BallSegment Segment = Segments.get(Index);
            if (Segment.StartTime() <= Time) {
                return Segment;
            }
        }
        return Segments.get(0);
    }

    @Nullable
    public Vec3 positionAt(double Time) {
        BallSegment Segment = at(Time);
        return Segment == null ? null : Segment.positionAt(Time);
    }

    public Vec3 velocityAt(double Time) {
        BallSegment Segment = at(Time);
        if (Segment == null || Segment.isTerminal() && Time >= Segment.endTime()) {
            return Vec3.ZERO;
        }
        return Segment.velocityAt(Time);
    }

    public double speedAt(double Time) {
        return velocityAt(Time).length();
    }

    public double endTime() {
        return Segments.isEmpty() ? Double.NEGATIVE_INFINITY : last().endTime();
    }

    public List<BallSegment> endingBetween(double After, double Until) {
        List<BallSegment> Found = new ArrayList<>();
        for (int Index = 0; Index < Segments.size(); Index++) {
            BallSegment Segment = Segments.get(Index);
            double End = Segment.endTime();
            boolean Cut = Index + 1 < Segments.size() && Segments.get(Index + 1).StartTime() < End - CutSlack;
            if (!Cut && End > After && End <= Until) {
                Found.add(Segment);
            }
        }
        return Found;
    }
}
