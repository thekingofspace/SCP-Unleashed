package net.rai.scp_unleashed.scp002;

import net.minecraft.util.Mth;

public final class BulkheadMotion {
    public static final int Closed = 0;
    public static final int Opening = 1;
    public static final int Open = 2;
    public static final int Closing = 3;
    public static final int DogTicks = 3;
    public static final int UnseatEnd = 6;
    public static final int OpenTicks = 16;
    public static final int HissCue = 3;
    public static final int SwingCue = 6;
    public static final int PassableAt = 12;
    public static final int CloseSwingTicks = 7;
    public static final int SeatEnd = 9;
    public static final int LatchStart = 10;
    public static final int CloseTicks = 12;
    public static final int SlamCue = 9;
    public static final int LatchCue = 10;
    public static final int LateCue = 4;
    public static final float PivotX = -0.5f;
    public static final float PivotZ = -3.0f;
    public static final float UnseatDepth = 8.0f;
    public static final float SwingDegrees = 90.0f;
    public static final float DogTravel = 2.5f;
    public static final float WheelX = 8.0f;
    public static final float WheelY = 20.5f;
    public static final float WheelTurn = 180.0f;

    private BulkheadMotion() {
    }

    public record Pose(float Dogs, float Unseat, float Swing) {
        public float swingDegrees() {
            return Swing * SwingDegrees;
        }

        public float unseatPixels() {
            return Unseat * UnseatDepth;
        }

        public float dogPixels() {
            return Dogs * DogTravel;
        }

        public float wheelDegrees() {
            return Dogs * WheelTurn;
        }
    }

    private static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    private static float accelerate(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped;
    }

    public static Pose pose(int Motion, float Elapsed) {
        return switch (Motion) {
            case Opening -> new Pose(smooth(Elapsed / DogTicks), smooth((Elapsed - DogTicks) / (UnseatEnd - DogTicks)),
                    smooth((Elapsed - UnseatEnd) / (OpenTicks - UnseatEnd)));
            case Open -> new Pose(1.0f, 1.0f, 1.0f);
            case Closing -> new Pose(1.0f - smooth((Elapsed - LatchStart) / (CloseTicks - LatchStart)),
                    1.0f - accelerate((Elapsed - CloseSwingTicks) / (SeatEnd - CloseSwingTicks)), 1.0f - accelerate(Elapsed / CloseSwingTicks));
            default -> new Pose(0.0f, 0.0f, 0.0f);
        };
    }
}
