package net.rai.scp_unleashed.scp999.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp999.Scp999Blob;
import net.rai.scp_unleashed.scp999.Scp999Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp999GeoModel extends SharedGeoModel<Scp999Blob> {
    public static final ResourceLocation ModelPath = Scp999Registry.location("geo/entity/scp999/scp999_blob.geo.json");
    public static final ResourceLocation TexturePath = Scp999Registry.location("textures/entity/scp999/scp999_blob.png");
    public static final ResourceLocation AnimationPath = Scp999Registry.location("animations/entity/scp999/scp999_blob.animation.json");
    public static final float EyeReachX = 0.8f;
    public static final float EyeReachUp = 0.5f;
    public static final float EyeReachDown = 0.75f;
    public static final float EyeScaleLow = 0.75f;
    public static final float EyeScaleHigh = 2.1f;
    public static final float EyeWidthHigh = 1.0f;
    public static final float Closed = 0.15f;
    public static final float Squint = 0.35f;
    public static final float Drowsy = 0.55f;
    public static final float Wide = 1.12f;
    public static final float Tremble = 0.12f;

    @Override
    public ResourceLocation getModelResource(Scp999Blob Blob) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp999Blob Blob) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp999Blob Blob) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp999Blob Blob, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    public static float openness(Scp999Blob Blob) {
        if (Blob.isDeadOrDying() || Blob.isNapping()) {
            return Closed;
        }
        double Elapsed = Blob.actionElapsed();
        String Clip = Blob.actionClip();
        if ("wake".equals(Clip)) {
            return Mth.lerp((float) Mth.clamp(Elapsed / 12.0, 0.0, 1.0), Closed, 1.0f);
        }
        if ("pet".equals(Clip) || "hug".equals(Clip) || "nuzzle".equals(Clip) || "shape_heart".equals(Clip) || "eat".equals(Clip) && Elapsed > 9.0 && Elapsed < 26.0) {
            return Squint;
        }
        if ("hum".equals(Clip)) {
            return Drowsy;
        }
        return Blob.eyes().blinking() ? Closed : 1.0f;
    }

    public static float wideness(Scp999Blob Blob) {
        String Clip = Blob.actionClip();
        if (Blob.isFlat() || "excited".equals(Clip) || "greet".equals(Clip) || "peek".equals(Clip)) {
            return Wide;
        }
        return 1.0f;
    }

    @Override
    public void setCustomAnimations(Scp999Blob Blob, long InstanceId, AnimationState<Scp999Blob> Animated) {
        GeoBone Body = getAnimationProcessor().getBone("body");
        GeoBone Left = getAnimationProcessor().getBone("eye_left");
        GeoBone Right = getAnimationProcessor().getBone("eye_right");
        if (Body == null || Left == null || Right == null) {
            return;
        }
        float Partial = Blob.renderPartial();
        float LookX = Blob.eyes().glanceX(Partial);
        float LookY = Blob.eyes().glanceY(Partial);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Data != null && Blob.isAlive()) {
            LookX -= Data.netHeadYaw() / Scp999Blob.LookTurn;
            LookY -= Data.headPitch() / Scp999Blob.LookPitch;
        }
        String Clip = Blob.actionClip();
        if ("peek".equals(Clip)) {
            LookX = Mth.sin((float) Blob.actionElapsed() * 0.45f);
            LookY = 0.4f;
        }
        if (Blob.isFlat() && !"peek".equals(Clip)) {
            LookX += Mth.sin((Blob.tickCount + Partial) * 2.7f) * Tremble;
        }
        float Open = openness(Blob);
        float Wider = wideness(Blob);
        float ScaleY = Mth.clamp(1.0f / Math.max(Body.getScaleY(), 1.0e-3f), EyeScaleLow, EyeScaleHigh) * Open * Wider;
        float ScaleX = Mth.clamp(1.0f / Math.max(Body.getScaleX(), 1.0e-3f), EyeScaleLow, EyeWidthHigh);
        float OffsetX = Mth.clamp(LookX, -1.0f, 1.0f) * EyeReachX;
        float ClampedY = Mth.clamp(LookY, -1.0f, 1.0f);
        float OffsetY = ClampedY * (ClampedY > 0.0f ? EyeReachUp : EyeReachDown);
        for (GeoBone Eye : new GeoBone[]{Left, Right}) {
            Eye.setPosX(OffsetX);
            Eye.setPosY(OffsetY);
            Eye.setPosZ(0.0f);
            Eye.setScaleX(ScaleX);
            Eye.setScaleY(ScaleY);
            Eye.setScaleZ(1.0f);
        }
    }
}
