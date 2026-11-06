package net.rai.scp_unleashed.scp018;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

public record BallSegment(Vec3 Start, Vec3 Velocity, double StartTime, double Duration, int Kind, int Mode, Vec3 Normal, int Target) {
    public static final int KindSplit = 0;
    public static final int KindBlock = 1;
    public static final int KindEntity = 2;
    public static final int KindExplode = 3;
    public static final int KindDrop = 4;
    public static final int KindStop = 5;
    public static final int KindSplash = 6;
    public static final int KindFloat = 7;
    public static final int ModeAir = 0;
    public static final int ModeWater = 1;
    public static final int ModeFloat = 2;
    public static final double TinyDuration = 1.0e-6;
    public static final double FloatTicks = 1.0e6;
    public static final StreamCodec<ByteBuf, BallSegment> SegmentCodec = StreamCodec.of(BallSegment::write, BallSegment::read);

    public static BallSegment of(Vec3 Start, Vec3 Velocity, double StartTime, double Duration, int Kind, Vec3 Normal, int Target) {
        return new BallSegment(Start, Velocity, StartTime, Math.max(0.0, Duration), Kind, ModeAir, Normal, Target);
    }

    public static BallSegment water(Vec3 Start, Vec3 Velocity, double StartTime, double Duration, int Kind, Vec3 Normal, int Target) {
        return new BallSegment(Start, Velocity, StartTime, Math.max(0.0, Duration), Kind, ModeWater, Normal, Target);
    }

    public static BallSegment floating(Vec3 Start, double StartTime) {
        return new BallSegment(Start, Vec3.ZERO, StartTime, FloatTicks, KindFloat, ModeFloat, Vec3.ZERO, -1);
    }

    public static BallSegment resting(Vec3 Start, double StartTime, int Kind, int Mode) {
        return new BallSegment(Start, Vec3.ZERO, StartTime, 0.0, Kind, Mode, Vec3.ZERO, -1);
    }

    public double endTime() {
        return StartTime + Duration;
    }

    public double local(double Time) {
        return Math.max(0.0, Math.min(Duration, Time - StartTime));
    }

    public boolean inWater() {
        return Mode != ModeAir;
    }

    public boolean isFloating() {
        return Mode == ModeFloat;
    }

    public Vec3 positionAtLocal(double Tau) {
        if (Mode == ModeFloat) {
            return Start;
        }
        if (Mode == ModeWater) {
            return Start.add(Velocity.scale(Tau));
        }
        return new Vec3(Start.x + Velocity.x * Tau, Start.y + Velocity.y * Tau - 0.5 * BallTuning.Gravity * Tau * Tau, Start.z + Velocity.z * Tau);
    }

    public Vec3 velocityAtLocal(double Tau) {
        if (Mode == ModeFloat) {
            return Vec3.ZERO;
        }
        if (Mode == ModeWater) {
            return Velocity;
        }
        return new Vec3(Velocity.x, Velocity.y - BallTuning.Gravity * Tau, Velocity.z);
    }

    public Vec3 positionAt(double Time) {
        return positionAtLocal(local(Time));
    }

    public Vec3 velocityAt(double Time) {
        return velocityAtLocal(local(Time));
    }

    public Vec3 end() {
        return positionAtLocal(Duration);
    }

    public Vec3 endVelocity() {
        return velocityAtLocal(Duration);
    }

    public double speed() {
        return Velocity.length();
    }

    public boolean isBounce() {
        return Kind == KindBlock || Kind == KindEntity;
    }

    public boolean isTerminal() {
        return Kind == KindExplode || Kind == KindDrop || Kind == KindStop;
    }

    public BallSegment shifted(double Offset) {
        return new BallSegment(Start, Velocity, StartTime + Offset, Duration, Kind, Mode, Normal, Target);
    }

    private static void write(ByteBuf Buffer, BallSegment Segment) {
        Vec3 End = Segment.end();
        Buffer.writeDouble(Segment.Start.x);
        Buffer.writeDouble(Segment.Start.y);
        Buffer.writeDouble(Segment.Start.z);
        Buffer.writeDouble(End.x);
        Buffer.writeDouble(End.y);
        Buffer.writeDouble(End.z);
        Buffer.writeDouble(Segment.StartTime);
        Buffer.writeFloat((float) Segment.Duration);
        Buffer.writeFloat((float) Segment.speed());
        Buffer.writeByte(Segment.Kind);
        Buffer.writeByte(Segment.Mode);
    }

    private static BallSegment read(ByteBuf Buffer) {
        Vec3 Start = new Vec3(Buffer.readDouble(), Buffer.readDouble(), Buffer.readDouble());
        Vec3 End = new Vec3(Buffer.readDouble(), Buffer.readDouble(), Buffer.readDouble());
        double StartTime = Buffer.readDouble();
        double Duration = Buffer.readFloat();
        Buffer.readFloat();
        int Kind = Buffer.readByte();
        int Mode = Buffer.readByte();
        Vec3 Velocity = Vec3.ZERO;
        if (Mode != ModeFloat && Duration > TinyDuration) {
            Vec3 Chord = End.subtract(Start).scale(1.0 / Duration);
            Velocity = Mode == ModeWater ? Chord : new Vec3(Chord.x, Chord.y + 0.5 * BallTuning.Gravity * Duration, Chord.z);
        }
        return new BallSegment(Start, Velocity, StartTime, Duration, Kind, Mode, Vec3.ZERO, -1);
    }
}
