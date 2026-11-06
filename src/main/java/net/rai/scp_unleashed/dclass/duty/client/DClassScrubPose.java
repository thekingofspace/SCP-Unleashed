package net.rai.scp_unleashed.dclass.duty.client;

import net.minecraft.util.Mth;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.duty.DClassDuties;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.function.Function;

public final class DClassScrubPose {
    public static final float Lean = 0.95f;
    public static final float Drop = 1.6f;
    public static final float HeadLift = 0.8f;
    public static final float Reach = 1.4f;
    public static final float Inward = 0.4f;
    public static final float Stroke = 0.2f;
    public static final float Sway = 0.08f;
    public static final float Tempo = 0.42f;
    public static final float Stride = 0.55f;

    private DClassScrubPose() {
    }

    public static boolean active(DClassLike Owner) {
        return Owner.action() == DClassDuties.ActionScrubbing && !Owner.isRestingPose() && !Owner.isSpecimen() && !Owner.isPuppet();
    }

    public static void apply(Function<String, GeoBone> Lookup, DClassLike Owner, float Partial) {
        if (!active(Owner)) {
            return;
        }
        float Phase = (Owner.tickCount + Partial) * Tempo + Owner.getId() * 0.7f;
        float Swing = Mth.sin(Phase);
        float Back = Mth.cos(Phase);
        GeoBone Waist = Lookup.apply("waist");
        if (Waist != null) {
            Waist.setRotX(-Lean);
            Waist.setRotY(Swing * Sway * 0.5f);
            Waist.setRotZ(0.0f);
            Waist.setPosY(-Drop);
        }
        GeoBone Body = Lookup.apply("body");
        if (Body != null) {
            Body.setRotY(Swing * Sway);
        }
        GeoBone Head = Lookup.apply("head");
        if (Head != null) {
            Head.setRotX(Head.getRotX() + HeadLift);
        }
        leg(Lookup.apply("right_leg"), Stride, 0.06f);
        leg(Lookup.apply("left_leg"), -Stride * 0.8f, -0.06f);
        arm(Lookup.apply("right_arm"), Reach + Swing * Stroke, Inward, Back * Sway);
        arm(Lookup.apply("left_arm"), Reach + Mth.sin(Phase + 0.6f) * Stroke, -Inward, Back * Sway);
    }

    private static void leg(GeoBone Bone, float Pitch, float Roll) {
        if (Bone != null) {
            Bone.setRotX(Pitch);
            Bone.setRotY(0.0f);
            Bone.setRotZ(Roll);
            Bone.setPosY(-Drop);
        }
    }

    private static void arm(GeoBone Bone, float Pitch, float Turn, float Roll) {
        if (Bone != null) {
            Bone.setRotX(Pitch);
            Bone.setRotY(Turn);
            Bone.setRotZ(Roll);
        }
    }
}
