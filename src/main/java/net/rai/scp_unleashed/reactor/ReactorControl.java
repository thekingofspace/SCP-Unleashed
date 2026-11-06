package net.rai.scp_unleashed.reactor;

public final class ReactorControl {
    public static final int SetTarget = 1;
    public static final int SetAll = 2;
    public static final int Scram = 3;
    public static final int Offset = 64;
    public static final int Mask = 127;

    private ReactorControl() {
    }

    public record Command(int Action, int Dx, int Dy, int Dz, int Value) {
    }

    public static boolean fits(int Dx, int Dy, int Dz) {
        return Math.abs(Dx) < Offset && Math.abs(Dy) < Offset && Math.abs(Dz) < Offset;
    }

    public static int encode(int Action, int Dx, int Dy, int Dz, int Value) {
        return (Action & 7) << 28 | ((Dx + Offset) & Mask) << 21 | ((Dz + Offset) & Mask) << 14 | ((Dy + Offset) & Mask) << 7 | Math.max(0, Math.min(100, Value));
    }

    public static Command decode(int Id) {
        return new Command(Id >>> 28 & 7, (Id >>> 21 & Mask) - Offset, (Id >>> 7 & Mask) - Offset, (Id >>> 14 & Mask) - Offset, Id & Mask);
    }
}
