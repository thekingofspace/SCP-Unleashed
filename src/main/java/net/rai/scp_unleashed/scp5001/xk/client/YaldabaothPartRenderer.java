package net.rai.scp_unleashed.scp5001.xk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.ArrayList;
import java.util.List;

public final class YaldabaothPartRenderer implements GeoRenderer<YaldabaothPart> {
    public static final float BlinkScale = 0.12f;
    private final YaldabaothPart Part;
    private final YaldabaothPartModel Model = new YaldabaothPartModel();
    private BakedGeoModel Baked;
    private final List<GeoBone> Eyes = new ArrayList<>();
    private final List<Boolean> EyeAlongZ = new ArrayList<>();
    private GeoBone JawTop;
    private GeoBone JawBottom;
    private GeoBone JawLeft;
    private GeoBone JawRight;
    private final GeoBone[] Lips = new GeoBone[4];
    private GeoBone Tip;

    public record Pose(float Jaw, float LookYaw, float LookPitch, int Instance, long Clock, float Partial, boolean Shut, int Glow) {
    }

    public YaldabaothPartRenderer(YaldabaothPart.Kind Shape) {
        Part = new YaldabaothPart(Shape);
    }

    private boolean bake() {
        if (Baked != null) {
            return true;
        }
        try {
            Baked = Model.getBakedModel(Part.model());
        } catch (RuntimeException Problem) {
            return false;
        }
        if (Baked == null) {
            return false;
        }
        collect(Baked.topLevelBones());
        return true;
    }

    private void collect(List<GeoBone> Bones) {
        for (GeoBone Bone : Bones) {
            String Name = Bone.getName();
            if (Name.startsWith("eye_")) {
                Eyes.add(Bone);
                EyeAlongZ.add(Name.startsWith("eye_z_"));
            }
            switch (Name) {
                case "jaw_top" -> JawTop = Bone;
                case "jaw_bottom" -> JawBottom = Bone;
                case "jaw_left" -> JawLeft = Bone;
                case "jaw_right" -> JawRight = Bone;
                case "lip_l_up" -> Lips[0] = Bone;
                case "lip_l_down" -> Lips[1] = Bone;
                case "lip_r_up" -> Lips[2] = Bone;
                case "lip_r_down" -> Lips[3] = Bone;
                case "tip" -> Tip = Bone;
                default -> {
                }
            }
            collect(Bone.getChildBones());
        }
    }

    private static float hash(int A, int B) {
        int Value = A * 73856093 ^ B * 19349663;
        Value ^= Value >>> 13;
        Value *= 0x5bd1e995;
        Value ^= Value >>> 15;
        return (Value & 0xFFFF) / 65535.0f;
    }

    private void pose(Pose Shape) {
        float Time = Shape.Clock() + Shape.Partial();
        float Yaw = Mth.clamp(Shape.LookYaw(), -0.45f, 0.45f);
        float Pitch = Mth.clamp(Shape.LookPitch(), -0.4f, 0.4f);
        for (int Index = 0; Index < Eyes.size(); Index++) {
            GeoBone Eye = Eyes.get(Index);
            float Period = 90.0f + hash(Index, Shape.Instance()) * 140.0f;
            float Phase = (Time + hash(Shape.Instance(), Index + 7) * Period) % Period;
            boolean Closed = Shape.Shut() || Phase < 3.5f;
            float Lid = Closed ? BlinkScale : Phase < 6.0f ? 0.55f : 1.0f;
            Eye.setScaleX(1.0f);
            Eye.setScaleY(EyeAlongZ.get(Index) ? 1.0f : Lid);
            Eye.setScaleZ(EyeAlongZ.get(Index) ? Lid : 1.0f);
            float Drift = (float) Math.sin(Time * 0.05f + Index * 1.7f) * 0.08f;
            Eye.setRotX(Pitch + Drift);
            Eye.setRotY(Yaw - Drift);
            Eye.setRotZ(0.0f);
        }
        float Open = Mth.clamp(Shape.Jaw(), 0.0f, 1.0f);
        if (JawTop != null) {
            JawTop.setRotX(-Mth.DEG_TO_RAD * 8.0f * Open);
        }
        if (JawBottom != null) {
            JawBottom.setRotX(Mth.DEG_TO_RAD * 32.0f * Open);
        }
        if (JawLeft != null) {
            JawLeft.setRotY(-Mth.DEG_TO_RAD * 24.0f * Open);
        }
        if (JawRight != null) {
            JawRight.setRotY(Mth.DEG_TO_RAD * 24.0f * Open);
        }
        if (Lips[0] != null) {
            float Maw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(Time * 0.06f + Shape.Instance() * 1.3f));
            Maw = Math.max(Maw * 0.8f, Open);
            Lips[0].setRotZ(Mth.DEG_TO_RAD * -28.0f * Maw);
            Lips[1].setRotZ(Mth.DEG_TO_RAD * 26.0f * Maw);
            Lips[2].setRotZ(Mth.DEG_TO_RAD * 28.0f * Maw);
            Lips[3].setRotZ(Mth.DEG_TO_RAD * -26.0f * Maw);
        }
        if (Tip != null) {
            Tip.setRotY((float) Math.sin(Time * 0.07f) * 0.35f);
            Tip.setRotX((float) Math.sin(Time * 0.05f + 1.0f) * 0.2f);
        }
    }

    public void draw(PoseStack Stack, MultiBufferSource Buffers, int Light, Pose Shape) {
        if (!bake()) {
            return;
        }
        pose(Shape);
        RenderType Body = RenderType.entityCutoutNoCull(Part.texture());
        VertexConsumer Buffer = Buffers.getBuffer(Body);
        for (GeoBone Bone : Baked.topLevelBones()) {
            renderRecursively(Stack, Part, Bone, Body, Buffers, Buffer, true, Shape.Partial(), Light, OverlayTexture.NO_OVERLAY, -1);
        }
        if (Eyes.isEmpty() && Tip == null) {
            return;
        }
        RenderType Glow = RenderType.eyes(Part.glow());
        VertexConsumer GlowBuffer = Buffers.getBuffer(Glow);
        for (GeoBone Bone : Baked.topLevelBones()) {
            renderRecursively(Stack, Part, Bone, Glow, Buffers, GlowBuffer, true, Shape.Partial(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Shape.Glow());
        }
    }

    @Override
    public GeoModel<YaldabaothPart> getGeoModel() {
        return Model;
    }

    @Override
    public YaldabaothPart getAnimatable() {
        return Part;
    }

    @Override
    public List<GeoRenderLayer<YaldabaothPart>> getRenderLayers() {
        return List.of();
    }

    @Override
    public void fireCompileRenderLayersEvent() {
    }

    @Override
    public boolean firePreRenderEvent(PoseStack Stack, BakedGeoModel Made, MultiBufferSource Buffers, float Partial, int Light) {
        return true;
    }

    @Override
    public void firePostRenderEvent(PoseStack Stack, BakedGeoModel Made, MultiBufferSource Buffers, float Partial, int Light) {
    }

    @Override
    public void updateAnimatedTextureFrame(YaldabaothPart Owner) {
    }
}
