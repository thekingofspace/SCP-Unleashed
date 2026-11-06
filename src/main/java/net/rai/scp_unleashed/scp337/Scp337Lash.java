package net.rai.scp_unleashed.scp337;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record Scp337Lash(int Target, Vec3 Aim, int Start, long Seed, boolean Real) {
    public static final int Ticks = 12;
    public static final int HitTick = 4;
    public static final int HoldTicks = 2;
    public static final int Segments = 16;
    public static final double BaseWidth = 0.17;
    public static final double TipWidth = 0.03;
    public static final int Frays = 2;
    public static final double FrayShare = 0.42;
    public static final double FrayBase = 0.03;
    public static final double FraySpread = 0.16;
    public static final double FrayTwist = 5.0;
    public static final double CoreHeight = 0.72;
    public static final double CoreSpread = 0.45;
    public static final double OriginReach = 0.55;
    public static final double CurlShare = 0.11;
    public static final double CoilShare = 0.17;
    public static final double SagShare = 0.12;
    public static final double FlickShare = 0.07;
    private static final Vec3 Up = new Vec3(0.0, 1.0, 0.0);

    public CompoundTag save() {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("T", Target);
        Tag.putDouble("X", Aim.x);
        Tag.putDouble("Y", Aim.y);
        Tag.putDouble("Z", Aim.z);
        Tag.putInt("S", Start);
        Tag.putLong("K", Seed);
        Tag.putBoolean("R", Real);
        return Tag;
    }

    public static Scp337Lash load(CompoundTag Tag) {
        return new Scp337Lash(Tag.getInt("T"), new Vec3(Tag.getDouble("X"), Tag.getDouble("Y"), Tag.getDouble("Z")), Tag.getInt("S"), Tag.getLong("K"), Tag.getBoolean("R"));
    }

    public static double share(long Seed, int Salt) {
        long Mixed = Seed * 0x9E3779B97F4A7C15L + Salt * 0xBF58476D1CE4E5B9L;
        Mixed ^= Mixed >>> 31;
        Mixed *= 0x94D049BB133111EBL;
        Mixed ^= Mixed >>> 29;
        return (Mixed >>> 11) * 0x1.0p-53;
    }

    public static Vec3 origin(Vec3 Base, Vec3 Aim, long Seed) {
        Vec3 Centre = Base.add(0.0, CoreHeight + CoreSpread * share(Seed, 1), 0.0);
        Vec3 Toward = new Vec3(Aim.x - Centre.x, 0.0, Aim.z - Centre.z);
        if (Toward.lengthSqr() < 1.0e-6) {
            Toward = new Vec3(1.0, 0.0, 0.0);
        }
        Toward = Toward.normalize();
        Vec3 Side = new Vec3(-Toward.z, 0.0, Toward.x);
        return Centre.add(Toward.scale(OriginReach)).add(Side.scale((share(Seed, 2) - 0.5) * 0.7)).add(0.0, (share(Seed, 3) - 0.5) * 0.3, 0.0);
    }

    public static double extension(double Time) {
        if (Time <= 0.0) {
            return 0.0;
        }
        if (Time < HitTick) {
            double Left = 1.0 - Time / HitTick;
            return 1.0 - Left * Left * Left;
        }
        if (Time < HitTick + HoldTicks) {
            return 1.0;
        }
        double Share = (Time - HitTick - HoldTicks) / (Ticks - HitTick - HoldTicks);
        return 1.0 - smooth(Share);
    }

    public static double tension(double Time) {
        if (Time < HitTick) {
            return 1.0 - Time / HitTick;
        }
        return 0.0;
    }

    public static double slack(double Time) {
        if (Time < HitTick + HoldTicks) {
            return 0.0;
        }
        return Mth.clamp((Time - HitTick - HoldTicks) / (Ticks - HitTick - HoldTicks), 0.0, 1.0);
    }

    public static double flick(double Time) {
        double Offset = Time - HitTick;
        if (Offset < -1.0 || Offset > 1.5) {
            return 0.0;
        }
        return Math.sin(Math.PI * (Offset + 1.0) / 2.5);
    }

    public static double smooth(double Value) {
        double Clamped = Mth.clamp(Value, 0.0, 1.0);
        return Clamped * Clamped * (3.0 - 2.0 * Clamped);
    }

    public static Vec3 point(Vec3 From, Vec3 To, double Time, double Along, long Seed) {
        Vec3 Span = To.subtract(From);
        double Length = Span.length();
        if (Length < 1.0e-4) {
            return From;
        }
        Vec3 Forward = Span.scale(1.0 / Length);
        Vec3 Side = Forward.cross(Up);
        if (Side.lengthSqr() < 1.0e-6) {
            Side = new Vec3(1.0, 0.0, 0.0);
        }
        Side = Side.normalize();
        Vec3 Lift = Side.cross(Forward).normalize();
        double Reach = extension(Time);
        double Phase = share(Seed, 4) * Math.PI * 2.0;
        double Plane = Phase + Along * 1.6;
        double Amplitude = Length * (CurlShare * tension(Time) + CoilShare * slack(Time));
        double Lateral = Amplitude * Math.sin(Math.PI * Along) * Math.sin(Math.PI * 2.0 * (1.3 * Along - Time / 6.0) + Phase);
        double Flick = Length * FlickShare * flick(Time) * Along * Along * Along * Along * (share(Seed, 5) < 0.5 ? -1.0 : 1.0);
        double Sag = Length * SagShare * slack(Time) * Math.sin(Math.PI * Along);
        Vec3 Curl = Side.scale(Math.cos(Plane)).add(Lift.scale(Math.sin(Plane))).scale(Lateral);
        return From.add(Forward.scale(Along * Reach * Length)).add(Curl).add(Side.scale(Flick)).add(0.0, -Sag, 0.0);
    }

    public static double width(double Along) {
        return BaseWidth * (1.0 - Along) + TipWidth * Along;
    }

    public static double frayReach(double Along) {
        return FrayBase + FraySpread * Along * Along;
    }

    public static double frayAngle(int Strand, double Along, long Seed) {
        return Along * FrayTwist + Strand * Math.PI + share(Seed, 6) * Math.PI * 2.0;
    }
}
