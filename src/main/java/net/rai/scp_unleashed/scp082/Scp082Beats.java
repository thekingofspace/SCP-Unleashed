package net.rai.scp_unleashed.scp082;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Scp082Beats {
    public static final int GrabTicks = 18;
    public static final int GrabContact = 8;
    public static final int HoldTicks = 50;
    public static final int EatTicks = 52;
    public static final int RaiseEnd = 12;
    public static final int BiteTick = 15;
    public static final int BurpTick = 44;
    public static final int SnackTicks = 34;
    public static final int SnackPickup = 9;
    public static final int SnackRaise = 17;
    public static final int SnackBite = 21;
    public static final int SlamTicks = 22;
    public static final int SlamHit = 11;
    public static final int RoarTicks = 40;
    public static final int RoarCry = 5;
    public static final int StompTicks = 22;
    public static final int StompHit = 12;
    public static final int SmashTicks = 18;
    public static final int SmashHit = 9;
    public static final int FlinchTicks = 14;
    public static final double GroundForward = 1.35;
    public static final double GroundRight = 0.3;
    public static final double GroundUp = 0.0;
    public static final double HoldForward = 1.25;
    public static final double HoldRight = 0.45;
    public static final double HoldUp = 1.0;
    public static final double MouthForward = 0.95;
    public static final double MouthRight = 0.15;
    public static final double MouthUp = 1.45;
    public static final double MouthHeight = 2.3;
    public static final double MouthAhead = 0.75;
    public static final double SnackForward = 1.0;
    public static final double SnackRight = 0.25;
    public static final double SlamAhead = 1.9;

    private Scp082Beats() {
    }

    public static double smooth(double Value) {
        double Clamped = Mth.clamp(Value, 0.0, 1.0);
        return Clamped * Clamped * (3.0 - 2.0 * Clamped);
    }

    public static Vec3 groundPoint() {
        return new Vec3(GroundForward, GroundRight, GroundUp);
    }

    public static Vec3 holdPoint() {
        return new Vec3(HoldForward, HoldRight, HoldUp);
    }

    public static Vec3 mouthPoint() {
        return new Vec3(MouthForward, MouthRight, MouthUp);
    }

    public static Vec3 liftPoint(double Elapsed) {
        double Share = smooth((Elapsed - GrabContact) / (GrabTicks - GrabContact));
        return groundPoint().lerp(holdPoint(), Share);
    }

    public static Vec3 raisePoint(double Elapsed) {
        return holdPoint().lerp(mouthPoint(), smooth(Elapsed / RaiseEnd));
    }
}
