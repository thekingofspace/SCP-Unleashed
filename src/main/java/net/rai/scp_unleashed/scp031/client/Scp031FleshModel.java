package net.rai.scp_unleashed.scp031.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp031.Scp031Mimic;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp031FleshModel extends GeoModel<Scp031Mimic> {
    public static final ResourceLocation ModelPath = location("geo/entity/scp031/flesh.geo.json");
    public static final ResourceLocation TexturePath = location("textures/entity/scp031/flesh.png");
    public static final ResourceLocation AnimationPath = location("animations/entity/scp031/flesh.animation.json");
    public static final float StepBob = 1.2f;
    public static final float StepSway = 0.09f;
    public static final float StepLean = 0.06f;
    public static final float StepSquash = 0.05f;
    public static final float CrouchSquash = 0.85f;
    public static final float CrouchSpread = 1.08f;
    public static final float TwistLimit = 0.2f;
    public static final float HugBelly = 0.2f;
    public static final float HugChest = 0.24f;
    public static final float HugNeck = 0.12f;
    public static final float HugFold = 0.62f;
    public static final float HugFoldStretch = 1.5f;
    public static final float HugFoldReach = 2.5f;
    public static final float HugTilt = 0.2f;
    public static final float LookPitch = 0.5f;
    public static final float LookYaw = 0.87f;

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp031Mimic Mimic) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp031Mimic Mimic) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp031Mimic Mimic) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp031Mimic Mimic, long InstanceId, AnimationState<Scp031Mimic> Animated) {
        float Partial = Animated.getPartialTick();
        float Swing = Mimic.walkAnimation.position(Partial);
        float Amount = Mimic.isAlive() ? Math.min(1.0f, Mimic.walkAnimation.speed(Partial)) : 0.0f;
        float Age = Mimic.tickCount + Partial;
        float Attack = Mimic.getAttackAnim(Partial);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        float HeadYaw = Data == null ? 0.0f : -Data.netHeadYaw() * Mth.DEG_TO_RAD;
        float HeadPitch = Data == null ? 0.0f : -Data.headPitch() * Mth.DEG_TO_RAD;
        float Phase = Swing * 0.6662f;
        float Step = Math.abs(Mth.sin(Phase)) * Amount;
        float BellyX = StepLean * Amount;
        float BellyZ = Mth.sin(Phase) * StepSway * Amount;
        float ChestX = 0.0f;
        float ChestY = 0.0f;
        float ChestZ = -Mth.sin(Phase) * StepSway * 0.5f * Amount;
        float NeckX = 0.0f;
        float HeadX = Mth.clamp(HeadPitch, -LookPitch, LookPitch);
        float HeadY = Mth.clamp(HeadYaw, -LookYaw, LookYaw);
        float HeadZ = 0.0f;
        float RootSpread = 1.0f;
        float RootSquash = 1.0f;
        float FoldYaw = 0.0f;
        float FoldReach = 0.0f;
        float FoldStretch = 1.0f;
        if (Attack > 0.0f) {
            ChestY = Mth.sin(Mth.sqrt(Attack) * Mth.TWO_PI) * TwistLimit;
            NeckX = Mth.sin(Attack * Mth.PI) * 0.15f;
        }
        if (Mimic.isCrouching()) {
            RootSpread = CrouchSpread;
            RootSquash = CrouchSquash;
        }
        if (Mimic.isEmbracing()) {
            float Sway = Mth.sin(Age * 0.18f) * 0.04f;
            BellyX = HugBelly;
            ChestX = HugChest + Sway;
            NeckX = HugNeck;
            HeadZ = HugTilt;
            FoldYaw = HugFold;
            FoldReach = -HugFoldReach;
            FoldStretch = HugFoldStretch;
        }
        scale(bone("root"), RootSpread, RootSquash, RootSpread);
        scale(bone("base"), 1.0f + Step * StepSquash * 0.6f, 1.0f - Step * StepSquash, 1.0f + Step * StepSquash * 0.6f);
        turn(bone("belly"), BellyX, 0.0f, BellyZ, Step * StepBob, 0.0f);
        turn(bone("chest"), ChestX, ChestY, ChestZ, 0.0f, 0.0f);
        turn(bone("neck"), NeckX, 0.0f, 0.0f, 0.0f, 0.0f);
        turn(bone("head"), HeadX, HeadY, HeadZ, 0.0f, 0.0f);
        turn(bone("fold_right"), 0.0f, FoldYaw, 0.0f, 0.0f, FoldReach);
        turn(bone("fold_left"), 0.0f, -FoldYaw, 0.0f, 0.0f, FoldReach);
        scale(bone("fold_right"), 1.0f, 1.0f, FoldStretch);
        scale(bone("fold_left"), 1.0f, 1.0f, FoldStretch);
    }

    @Nullable
    private GeoBone bone(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    private static void turn(@Nullable GeoBone Bone, float X, float Y, float Z, float PosY, float PosZ) {
        if (Bone == null) {
            return;
        }
        Bone.setRotX(-X);
        Bone.setRotY(-Y);
        Bone.setRotZ(Z);
        Bone.setPosY(PosY);
        Bone.setPosZ(PosZ);
    }

    private static void scale(@Nullable GeoBone Bone, float X, float Y, float Z) {
        if (Bone == null) {
            return;
        }
        Bone.setScaleX(X);
        Bone.setScaleY(Y);
        Bone.setScaleZ(Z);
    }
}
