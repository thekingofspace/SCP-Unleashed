package net.rai.scp_unleashed.scp131.client;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

public final class Scp131ItemFace {
    public static final float Window = 26.0f;
    public static final float Saccade = 1.6f;
    public static final float ViewerChance = 0.4f;
    public static final float CentreChance = 0.15f;
    public static final float BlinkChance = 0.4f;
    public static final float SquintChance = 0.12f;
    public static final float SquintEase = 4.0f;
    public static final float ViewerGuiX = -0.5f;
    public static final float ViewerGuiY = 0.35f;
    public static final float ViewerHandX = 0.6f;
    public static final float ViewerHandY = 1.1f;
    public static final float SadWindow = 1.7f;
    public static final float BreathDepth = 0.015f;
    public static final float TipBobItem = 3.0f;
    public static final float WagChance = 0.25f;

    private Scp131ItemFace() {
    }

    public static float hash(long Seed, long Index, int Salt) {
        long Mixed = Seed * 6364136223846793005L + Index * 1442695040888963407L + Salt * 0x9E3779B97F4A7C15L;
        Mixed ^= Mixed >>> 33;
        Mixed *= 0xff51afd7ed558ccdL;
        Mixed ^= Mixed >>> 33;
        Mixed *= 0xc4ceb9fe1a85ec53L;
        Mixed ^= Mixed >>> 33;
        return (Mixed >>> 40) / (float) (1L << 24);
    }

    private static boolean viewerFacing(ItemDisplayContext Context) {
        return Context == ItemDisplayContext.GUI || Context.firstPerson();
    }

    private static float targetX(long Seed, long Slot, ItemDisplayContext Context) {
        float Roll = hash(Seed, Slot, 1);
        if (viewerFacing(Context) && Roll < ViewerChance) {
            return Context == ItemDisplayContext.GUI ? ViewerGuiX : ViewerHandX * (Context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1.0f : 1.0f);
        }
        if (Roll < ViewerChance + CentreChance) {
            return 0.0f;
        }
        return (hash(Seed, Slot, 2) * 2.0f - 1.0f) * Scp131Face.IrisRangeX;
    }

    private static float targetY(long Seed, long Slot, ItemDisplayContext Context) {
        float Roll = hash(Seed, Slot, 1);
        if (viewerFacing(Context) && Roll < ViewerChance) {
            return Context == ItemDisplayContext.GUI ? ViewerGuiY : ViewerHandY;
        }
        if (Roll < ViewerChance + CentreChance) {
            return 0.2f;
        }
        return (hash(Seed, Slot, 3) * 1.4f - 0.8f) * Scp131Face.IrisRangeY;
    }

    public static Scp131Pose pose(Scp131Pose Pose, long Seed, float Time, boolean Sad, ItemDisplayContext Context) {
        Pose.reset();
        float Span = Window * (Sad ? SadWindow : 1.0f);
        float Shifted = Time + hash(Seed, 0, 9) * Span;
        long Slot = (long) Math.floor(Shifted / Span);
        float Into = Shifted - Slot * Span;
        float Ease = Mth.clamp(Into / Saccade, 0.0f, 1.0f);
        Ease = Ease * Ease * (3.0f - 2.0f * Ease);
        float FromX = targetX(Seed, Slot - 1, Context);
        float FromY = targetY(Seed, Slot - 1, Context);
        float ToX = targetX(Seed, Slot, Context);
        float ToY = targetY(Seed, Slot, Context);
        boolean Squint = !Sad && hash(Seed, Slot, 5) < SquintChance;
        float SquintIn = !Sad && hash(Seed, Slot - 1, 5) < SquintChance ? 1.0f : Mth.clamp(Into / SquintEase, 0.0f, 1.0f);
        float SquintOut = !Sad && hash(Seed, Slot + 1, 5) < SquintChance ? 1.0f : Mth.clamp((Span - Into) / SquintEase, 0.0f, 1.0f);
        float SquintShare = Squint ? Math.min(SquintIn, SquintOut) : 0.0f;
        if (Sad) {
            Pose.IrisX = Mth.lerp(Ease, FromX, ToX) * 0.3f;
            Pose.IrisY = -Scp131Face.IrisRangeY * 0.95f;
            Pose.LidTop = Scp131Face.LidSad;
            Pose.LidBottom = Scp131Face.LowerSad;
            Pose.Pupil = Scp131Face.PupilSad;
        } else {
            Pose.IrisX = Mth.lerp(Ease, FromX, ToX);
            Pose.IrisY = Mth.lerp(Ease, FromY, ToY);
            Pose.LidTop = Mth.lerp(SquintShare, Scp131Face.LidRest, Scp131Face.LidHappy);
            Pose.LidBottom = Scp131Face.LowerHappy * SquintShare;
            Pose.Pupil = Scp131Face.PupilRest;
        }
        if (hash(Seed, Slot, 4) < BlinkChance) {
            float BlinkStart = 4.0f + hash(Seed, Slot, 6) * (Span - 12.0f);
            float Blink = Scp131Face.blinkCurve((Into - BlinkStart) / (Sad ? Scp131Face.SadBlink : 1.0f));
            Pose.LidTop = Math.max(Pose.LidTop, Blink);
            Pose.LidBottom = Math.max(Pose.LidBottom, Blink * Scp131Face.LowerBlink);
        }
        boolean Wag = !Sad && hash(Seed, Slot, 7) < WagChance;
        for (int Joint = 0; Joint < Scp131Pose.Joints; Joint++) {
            float Weight = 0.5f + 0.5f * Joint / (Scp131Pose.Joints - 1.0f);
            float Bob = TipBobItem * Mth.sin(Time * Mth.TWO_PI / Scp131Face.TipBobTicks - Joint * 0.6f) * Weight;
            Pose.TipPitch[Joint] = (Sad ? Scp131Face.TipDroop : 0.0f) + Bob * (Sad ? 0.4f : 1.0f);
            Pose.TipRoll[Joint] = Wag ? Scp131Face.TipWag * 0.7f * Mth.sin(Time * Mth.TWO_PI / Scp131Face.TipWagTicks - Joint * 0.8f) * Weight : 0.0f;
        }
        Pose.Squash = (Sad ? Scp131Face.SadSquash : 0.0f) + Mth.sin(Time * Mth.TWO_PI / Scp131Face.BreathTicks) * BreathDepth;
        Pose.BodyPitch = Sad ? Scp131Face.SadLean * 0.6f : 0.0f;
        return Pose;
    }
}
