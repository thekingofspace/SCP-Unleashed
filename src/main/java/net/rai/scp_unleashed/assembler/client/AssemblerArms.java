package net.rai.scp_unleashed.assembler.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class AssemblerArms {
    public static final float ShoulderHeight = 2.5f;
    public static final float UpperLength = 5.0f;
    public static final float ForeLength = 4.5f;
    public static final float ToolLength = 2.5f;
    public static final float FingerOpen = 0.9f;
    public static final float FingerClosed = 0.55f;
    public static final Vec3 GripperBase = new Vec3(3.5, 1.5, 11.5);
    public static final Vec3 WelderBase = new Vec3(12.5, 1.5, 11.5);
    public static final Vec3 WorkCentre = new Vec3(8.0, 3.0, 7.0);
    public static final Joints Rest = new Joints(Mth.PI, 0.0f, Mth.HALF_PI, Mth.HALF_PI);
    public static final float GripAt = 0.22f;
    public static final float ReleaseAt = 0.52f;
    public static final float WeldedAt = 0.9f;
    public static final float SpinPerCycle = 180.0f;
    public static final float[][] WeldWindows = {{0.08f, 0.3f}, {0.66f, 0.9f}};
    public static final float[] GripperMoves = {0.0f, 0.3f, 0.58f};
    public static final float[] WelderMoves = {0.0f, 0.3f, 0.58f, 0.9f};
    public static final Vec3 PlacedPart = new Vec3(7.0, 3.3, 6.1);

    private static final Key[] GripperKeys = {
            new Key(0.0f, 4.6f, 7.6f, 9.0f, 0.0f),
            new Key(0.14f, 4.25f, 5.8f, 4.25f, 0.0f),
            new Key(0.2f, 4.25f, 2.75f, 4.25f, 0.0f),
            new Key(0.24f, 4.25f, 2.75f, 4.25f, 1.0f),
            new Key(0.3f, 4.25f, 5.8f, 4.25f, 1.0f),
            new Key(0.44f, 6.8f, 5.8f, 6.0f, 1.0f),
            new Key(0.5f, 7.0f, 3.0f, 6.1f, 1.0f),
            new Key(0.54f, 7.0f, 3.0f, 6.1f, 0.0f),
            new Key(0.58f, 6.6f, 5.8f, 6.4f, 0.0f),
            new Key(0.66f, 4.6f, 7.6f, 9.0f, 0.0f),
            new Key(1.0f, 4.6f, 7.6f, 9.0f, 0.0f)
    };
    private static final Key[] WelderKeys = {
            new Key(0.0f, 11.0f, 7.4f, 8.6f, 0.0f),
            new Key(0.04f, 9.6f, 6.6f, 6.6f, 0.0f),
            new Key(0.08f, 9.3f, 6.2f, 6.6f, 0.0f),
            new Key(0.3f, 9.3f, 4.0f, 6.9f, 0.0f),
            new Key(0.34f, 9.8f, 5.6f, 7.6f, 0.0f),
            new Key(0.4f, 11.0f, 7.4f, 8.6f, 0.0f),
            new Key(0.58f, 11.0f, 7.4f, 8.6f, 0.0f),
            new Key(0.62f, 9.4f, 5.6f, 8.3f, 0.0f),
            new Key(0.66f, 9.1f, 4.6f, 8.2f, 0.0f),
            new Key(0.9f, 7.5f, 4.6f, 8.7f, 0.0f),
            new Key(0.94f, 8.0f, 6.2f, 9.0f, 0.0f),
            new Key(1.0f, 11.0f, 7.4f, 8.6f, 0.0f)
    };

    private AssemblerArms() {
    }

    public static Target gripperTarget(float Phase) {
        return sample(GripperKeys, Phase);
    }

    public static Target welderTarget(float Phase) {
        return sample(WelderKeys, Phase);
    }

    public static boolean welding(float Phase) {
        for (float[] Window : WeldWindows) {
            if (Phase >= Window[0] && Phase < Window[1]) {
                return true;
            }
        }
        return false;
    }

    public static boolean holding(float Phase) {
        return Phase >= GripAt && Phase < ReleaseAt;
    }

    public static boolean partPlaced(float Phase) {
        return Phase >= ReleaseAt && Phase < WeldedAt;
    }

    public static float turntableAngle(int Cycles, float Phase) {
        return SpinPerCycle * (Cycles + Phase);
    }

    public static Vec3 placedPart(float Phase) {
        float Turn = SpinPerCycle * (Phase - ReleaseAt) * Mth.DEG_TO_RAD;
        double Dx = PlacedPart.x - WorkCentre.x;
        double Dz = PlacedPart.z - WorkCentre.z;
        return new Vec3(WorkCentre.x + Mth.cos(Turn) * Dx + Mth.sin(Turn) * Dz, PlacedPart.y, WorkCentre.z - Mth.sin(Turn) * Dx + Mth.cos(Turn) * Dz);
    }

    public static float smooth(float Value) {
        return Value * Value * (3.0f - 2.0f * Value);
    }

    private static Target sample(Key[] Keys, float Phase) {
        for (int Index = 0; Index + 1 < Keys.length; Index++) {
            Key From = Keys[Index];
            Key To = Keys[Index + 1];
            if (Phase <= To.Time()) {
                float Span = To.Time() - From.Time();
                float Mix = Span <= 0.0f ? 1.0f : smooth(Mth.clamp((Phase - From.Time()) / Span, 0.0f, 1.0f));
                return new Target(new Vec3(Mth.lerp(Mix, From.X(), To.X()), Mth.lerp(Mix, From.Y(), To.Y()), Mth.lerp(Mix, From.Z(), To.Z())),
                        Mth.lerp(Mix, From.Grip(), To.Grip()));
            }
        }
        Key Last = Keys[Keys.length - 1];
        return new Target(new Vec3(Last.X(), Last.Y(), Last.Z()), Last.Grip());
    }

    public static Joints solve(Vec3 Base, Vec3 Tip) {
        double WristX = Tip.x;
        double WristY = Tip.y + ToolLength;
        double WristZ = Tip.z;
        double Dx = WristX - Base.x;
        double Dz = WristZ - Base.z;
        float Yaw = (float) Math.atan2(Dx, Dz);
        double Reach = Math.sqrt(Dx * Dx + Dz * Dz);
        double Rise = WristY - (Base.y + ShoulderHeight);
        double Span = Mth.clamp(Math.sqrt(Reach * Reach + Rise * Rise), Math.abs(UpperLength - ForeLength) + 0.01, UpperLength + ForeLength - 0.01);
        double Lean = Math.atan2(Reach, Rise);
        double Inner = Math.acos(Mth.clamp((UpperLength * UpperLength + Span * Span - ForeLength * ForeLength) / (2.0 * UpperLength * Span), -1.0, 1.0));
        double Bend = Math.acos(Mth.clamp((UpperLength * UpperLength + ForeLength * ForeLength - Span * Span) / (2.0 * UpperLength * ForeLength), -1.0, 1.0));
        float Shoulder = (float) (Lean - Inner);
        float Elbow = (float) (Math.PI - Bend);
        float Wrist = Mth.PI - Shoulder - Elbow;
        return new Joints(Yaw, Shoulder, Elbow, Wrist);
    }

    public static Joints blend(Joints From, Joints To, float Mix) {
        float Yaw = From.Yaw() + Mth.wrapDegrees((To.Yaw() - From.Yaw()) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * Mix;
        return new Joints(Yaw, Mth.lerp(Mix, From.Shoulder(), To.Shoulder()), Mth.lerp(Mix, From.Elbow(), To.Elbow()), Mth.lerp(Mix, From.Wrist(), To.Wrist()));
    }

    public static Vec3 tip(Vec3 Base, Joints Arm) {
        double Forward = UpperLength * Math.sin(Arm.Shoulder()) + ForeLength * Math.sin(Arm.Shoulder() + Arm.Elbow())
                + ToolLength * Math.sin(Arm.Shoulder() + Arm.Elbow() + Arm.Wrist());
        double Up = ShoulderHeight + UpperLength * Math.cos(Arm.Shoulder()) + ForeLength * Math.cos(Arm.Shoulder() + Arm.Elbow())
                + ToolLength * Math.cos(Arm.Shoulder() + Arm.Elbow() + Arm.Wrist());
        return new Vec3(Base.x + Forward * Math.sin(Arm.Yaw()), Base.y + Up, Base.z + Forward * Math.cos(Arm.Yaw()));
    }

    public record Joints(float Yaw, float Shoulder, float Elbow, float Wrist) {
    }

    public record Target(Vec3 Tip, float Grip) {
    }

    private record Key(float Time, float X, float Y, float Z, float Grip) {
    }
}
