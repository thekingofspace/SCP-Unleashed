package net.rai.scp_unleashed.scp428.client;

import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp428.Scp428Instance;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.function.Function;

public final class Scp428Poses {
    public static final float HeadDroop = 0.34f;
    public static final float HeadLoll = 0.16f;
    public static final float Slouch = 0.14f;
    public static final float ArmHang = 0.12f;
    public static final float ArmSway = 0.07f;
    public static final float ArmSpread = 0.05f;
    public static final float LidFloor = 0.45f;
    public static final float RiseBend = 0.9f;
    public static final float RiseHead = 0.55f;
    public static final float TwitchHead = 0.5f;
    public static final float TwitchArm = 0.6f;
    public static final float HoldReach = 1.25f;
    public static final float HoldInward = 0.14f;
    public static final float HoldSpread = 0.08f;
    public static final float HoldLean = 0.22f;
    public static final float HoldTremble = 0.06f;
    public static final float HuddleLean = 0.38f;
    public static final float HuddleLook = 0.16f;
    public static final float HuddleReach = 0.72f;
    public static final float HuddleOut = 0.26f;
    public static final float HuddleSpread = 0.3f;
    public static final float GuardLean = 0.22f;
    public static final float GuardLook = 0.24f;
    public static final float GuardReach = 1.0f;
    public static final float GuardOut = 0.34f;
    public static final float GuardSpread = 0.22f;
    public static final float GuardTremble = 0.03f;
    public static final float BraceLean = 0.55f;
    public static final float BraceLook = 0.1f;
    public static final float BraceReach = 0.45f;
    public static final float BraceOut = -0.1f;
    public static final float BraceSpread = 0.12f;
    public static final float SwayRate = 0.0524f;
    public static final float SwayLean = 0.05f;
    public static final float SwayRoll = 0.045f;
    public static final float SwayArm = 0.07f;
    public static final float SwipeReach = 1.35f;
    public static final float SwipeLift = 0.25f;
    public static final float SwipeSweep = 0.55f;
    public static final float SwipeLean = 0.12f;
    public static final float AimReach = 1.48f;
    public static final float AimInward = 0.06f;
    public static final float AimSupport = 0.42f;

    private Scp428Poses() {
    }

    public record Stance(boolean Holding, float Swipe, boolean Aiming, byte Mode, float Rise, float Time, float Phase, float Shared, float Twitch) {
    }

    public static void pose(Function<String, GeoBone> Part, Stance Now) {
        float Time = Now.Time();
        float Phase = Now.Phase();
        float Rise = Now.Rise();
        if (Now.Holding()) {
            float Strain = Mth.sin(Time * 0.9f + Phase) * HoldTremble;
            add(Part, "waist", -HoldLean, 0.0f, 0.0f);
            set(Part, "right_arm", HoldReach + Strain, HoldInward, HoldSpread);
            set(Part, "left_arm", HoldReach - Strain, -HoldInward, -HoldSpread);
            eyes(Part);
            return;
        }
        float Swipe = Now.Swipe();
        if (Swipe >= 0.0f) {
            float Raise = Mth.sin(Mth.clamp(Swipe * 2.0f, 0.0f, 1.0f) * Mth.HALF_PI);
            float Across = Mth.clamp((Swipe - 0.25f) * 2.0f, 0.0f, 1.0f);
            add(Part, "waist", -SwipeLean * Raise, 0.0f, 0.0f);
            set(Part, "right_arm", (SwipeReach + SwipeLift * Mth.sin(Across * Mth.PI)) * Raise, SwipeSweep * (Across * 2.0f - 1.0f) * Raise, 0.0f);
            eyes(Part);
            return;
        }
        if (Now.Aiming()) {
            set(Part, "right_arm", AimReach, AimInward, 0.0f);
            set(Part, "left_arm", AimReach, -AimSupport, 0.0f);
            eyes(Part);
            return;
        }
        byte Mode = Now.Mode();
        if (Mode != Scp428Instance.Loose && Rise <= 0.0f) {
            huddle(Part, Mode, Now.Shared(), Time, Phase);
            eyes(Part);
            return;
        }
        float Twitch = Now.Twitch();
        add(Part, "waist", -Slouch + Rise * RiseBend, 0.0f, 0.0f);
        add(Part, "head", -HeadDroop + Rise * RiseHead, Mth.sin(Time * 0.031f + Phase) * 0.08f, HeadLoll * Mth.sin(Time * 0.045f + Phase) + Twitch * TwitchHead * (Phase > Mth.PI ? 1.0f : -1.0f));
        float Sway = ArmSway * Mth.sin(Time * 0.07f + Phase);
        add(Part, "right_arm", ArmHang + Sway + Rise * 0.6f + (Phase > Mth.PI ? Twitch * TwitchArm : 0.0f), 0.0f, ArmSpread);
        add(Part, "left_arm", ArmHang - Sway + Rise * 0.6f + (Phase > Mth.PI ? 0.0f : Twitch * TwitchArm), 0.0f, -ArmSpread);
        eyes(Part);
    }

    private static void huddle(Function<String, GeoBone> Part, byte Mode, float Shared, float Time, float Phase) {
        float Breath = Mth.sin(Shared * SwayRate);
        float Roll = Mth.sin(Shared * SwayRate * 0.5f + 1.3f);
        float Lean = Mode == Scp428Instance.Bracing ? BraceLean : Mode == Scp428Instance.Guarding ? GuardLean : HuddleLean;
        float Look = Mode == Scp428Instance.Bracing ? BraceLook : Mode == Scp428Instance.Guarding ? GuardLook : HuddleLook;
        float Reach = Mode == Scp428Instance.Bracing ? BraceReach : Mode == Scp428Instance.Guarding ? GuardReach : HuddleReach;
        float Out = Mode == Scp428Instance.Bracing ? BraceOut : Mode == Scp428Instance.Guarding ? GuardOut : HuddleOut;
        float Spread = Mode == Scp428Instance.Bracing ? BraceSpread : Mode == Scp428Instance.Guarding ? GuardSpread : HuddleSpread;
        float Tremble = Mode == Scp428Instance.Guarding ? GuardTremble * Mth.sin(Time * 0.6f + Phase) : 0.0f;
        add(Part, "waist", -(Lean + SwayLean * Breath), 0.0f, SwayRoll * Roll);
        add(Part, "head", Look, 0.0f, 0.0f);
        set(Part, "right_arm", Reach + SwayArm * Breath + Tremble, -Out, Spread);
        set(Part, "left_arm", Reach + SwayArm * Breath - Tremble, Out, -Spread);
    }

    private static void add(Function<String, GeoBone> Part, String Name, float X, float Y, float Z) {
        GeoBone Found = Part.apply(Name);
        if (Found != null) {
            Found.setRotX(Found.getRotX() + X);
            Found.setRotY(Found.getRotY() + Y);
            Found.setRotZ(Found.getRotZ() + Z);
        }
    }

    private static void set(Function<String, GeoBone> Part, String Name, float X, float Y, float Z) {
        GeoBone Found = Part.apply(Name);
        if (Found != null) {
            Found.setRotX(X);
            Found.setRotY(Y);
            Found.setRotZ(Z);
        }
    }

    private static void eyes(Function<String, GeoBone> Part) {
        for (String Name : new String[]{"right_lid", "left_lid"}) {
            GeoBone Found = Part.apply(Name);
            if (Found != null && Found.getScaleY() < LidFloor) {
                Found.setHidden(false);
                Found.setScaleY(LidFloor);
            }
        }
    }
}
