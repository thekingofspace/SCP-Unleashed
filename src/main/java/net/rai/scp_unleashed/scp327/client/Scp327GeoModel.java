package net.rai.scp_unleashed.scp327.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp327.Scp327Registry;
import net.rai.scp_unleashed.scp327.Scp327Siren;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class Scp327GeoModel extends GeoModel<Scp327Siren> {
    public static final ResourceLocation ModelPath = Scp327Registry.location("geo/entity/scp327/siren.geo.json");
    public static final ResourceLocation TexturePath = Scp327Registry.location("textures/entity/scp327/siren.png");
    public static final ResourceLocation AnimationPath = Scp327Registry.location("animations/entity/scp327/siren.animation.json");
    public static final String[] Spine = {"waist", "chest", "neck"};
    public static final float[] SpineShare = {0.35f, 0.4f, 0.25f};
    public static final String[] Tail = {"tail_0", "tail_1", "tail_2", "tail_3", "tail_4", "tail_5"};
    public static final float[] TailShare = {0.1f, 0.15f, 0.18f, 0.2f, 0.18f, 0.12f};
    public static final String[] Strands = {"inner", "middle", "outer"};
    public static final String[] Sides = {"left", "right"};
    public static final int HairSegments = 5;
    public static final String[] Lobes = {"fluke_left_0", "fluke_left_1", "fluke_right_0", "fluke_right_1"};
    public static final float BankGain = 2.5f;
    public static final float BankLimit = 25.0f;
    public static final float CurlGain = 1.4f;
    public static final float CurlLimit = 12.0f;
    public static final float HairYawGain = 2.4f;
    public static final float HairPitchGain = 3.0f;
    public static final float SwimPitchLimit = 45.0f;
    public static final float LookYawLimit = 45.0f;
    public static final float LookPitchLimit = 30.0f;
    public static final float BabyHead = 1.25f;

    @Override
    public ResourceLocation getModelResource(Scp327Siren Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp327Siren Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp327Siren Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp327Siren Creature, long InstanceId, AnimationState<Scp327Siren> Animated) {
        float Partial = Animated.getPartialTick();
        byte Current = Creature.mode();
        boolean Alive = !Creature.isDeadOrDying();
        boolean Swimming = Alive && Creature.isInWater() && (Current == Scp327Siren.ModeSwim || Current == Scp327Siren.ModeDrag || Current == Scp327Siren.ModeDive);
        float YawRate = Swimming ? Mth.lerp(Partial, Creature.ClientYawRateO, Creature.ClientYawRate) : 0.0f;
        float PitchRate = Swimming ? Mth.lerp(Partial, Creature.ClientPitchRateO, Creature.ClientPitchRate) : 0.0f;
        float BodyPitch = Swimming ? Mth.clamp(Mth.wrapDegrees(Creature.getViewXRot(Partial)), -SwimPitchLimit, SwimPitchLimit) : 0.0f;
        place("swim", BodyPitch, 0.0f, 0.0f);
        add("bank", 0.0f, 0.0f, bank(YawRate));
        float Curl = curl(YawRate);
        for (int Index = 0; Index < Spine.length; Index++) {
            add(Spine[Index], 0.0f, Curl * SpineShare[Index], 0.0f);
        }
        for (int Index = 0; Index < Tail.length; Index++) {
            add(Tail[Index], 0.0f, -Curl * TailShare[Index] * 2.0f, 0.0f);
        }
        for (String Lobe : Lobes) {
            add(Lobe, -PitchRate * 1.5f, 0.0f, 0.0f);
        }
        boolean Baby = Creature.isBaby();
        boolean Teen = Creature.stage() == Scp327Siren.StageTeen;
        for (String Strand : Strands) {
            for (String Side : Sides) {
                for (int Segment = 0; Segment < HairSegments; Segment++) {
                    GeoBone Hair = getAnimationProcessor().getBone("hair_" + Strand + "_" + Side + "_" + Segment);
                    if (Hair == null) {
                        continue;
                    }
                    Hair.setHidden(Baby || Teen && Segment > 0);
                    if (Segment > 0) {
                        float Grow = Segment / (float) HairSegments;
                        nudge(Hair, PitchRate * HairPitchGain * Grow, YawRate * HairYawGain * Grow, 0.0f);
                    }
                }
            }
        }
        GeoBone Head = getAnimationProcessor().getBone("head");
        if (Head != null) {
            float Scale = Baby ? BabyHead : 1.0f;
            Head.setScaleX(Scale);
            Head.setScaleY(Scale);
            Head.setScaleZ(Scale);
        }
        boolean Looks = Alive && (Current == Scp327Siren.ModeSing || Current == Scp327Siren.ModeBeached || Current == Scp327Siren.ModeBeachSing
                || Current == Scp327Siren.ModeSwim && Creature.flourish() == Scp327Siren.FlourishNone);
        float LookYaw = 0.0f;
        float LookPitch = 0.0f;
        if (Looks) {
            float BodyYaw = Mth.rotLerp(Partial, Creature.yBodyRotO, Creature.yBodyRot);
            float HeadYaw = Mth.rotLerp(Partial, Creature.yHeadRotO, Creature.yHeadRot);
            LookYaw = lookYaw(HeadYaw, BodyYaw);
            LookPitch = Swimming ? 0.0f : lookPitch(Creature.getViewXRot(Partial));
        }
        place("look", LookPitch, LookYaw, 0.0f);
    }

    public static float bank(float YawRate) {
        return Mth.clamp(-YawRate * BankGain, -BankLimit, BankLimit);
    }

    public static float curl(float YawRate) {
        return Mth.clamp(YawRate * CurlGain, -CurlLimit, CurlLimit);
    }

    public static float lookYaw(float HeadYaw, float BodyYaw) {
        return Mth.clamp(Mth.wrapDegrees(HeadYaw - BodyYaw), -LookYawLimit, LookYawLimit);
    }

    public static float lookPitch(float Pitch) {
        return Mth.clamp(Mth.wrapDegrees(Pitch), -LookPitchLimit, LookPitchLimit);
    }

    private void place(String Name, float PitchDegrees, float YawDegrees, float RollDegrees) {
        GeoBone Bone = getAnimationProcessor().getBone(Name);
        if (Bone != null) {
            Bone.setRotX(-PitchDegrees * Mth.DEG_TO_RAD);
            Bone.setRotY(-YawDegrees * Mth.DEG_TO_RAD);
            Bone.setRotZ(RollDegrees * Mth.DEG_TO_RAD);
        }
    }

    private void add(String Name, float PitchDegrees, float YawDegrees, float RollDegrees) {
        GeoBone Bone = getAnimationProcessor().getBone(Name);
        if (Bone != null) {
            nudge(Bone, PitchDegrees, YawDegrees, RollDegrees);
        }
    }

    private static void nudge(GeoBone Bone, float PitchDegrees, float YawDegrees, float RollDegrees) {
        Bone.setRotX(Bone.getRotX() - PitchDegrees * Mth.DEG_TO_RAD);
        Bone.setRotY(Bone.getRotY() - YawDegrees * Mth.DEG_TO_RAD);
        Bone.setRotZ(Bone.getRotZ() + RollDegrees * Mth.DEG_TO_RAD);
    }
}
