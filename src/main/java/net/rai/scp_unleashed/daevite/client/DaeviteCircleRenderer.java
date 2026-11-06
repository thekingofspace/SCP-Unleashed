package net.rai.scp_unleashed.daevite.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.entity.DaeviteCircle;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class DaeviteCircleRenderer extends EntityRenderer<DaeviteCircle> {
    public static final ResourceLocation Ring = texture("circle_ring");
    public static final ResourceLocation Sigil = texture("circle_sigil");
    public static final ResourceLocation Bind = texture("circle_bind");
    public static final ResourceLocation Small = texture("circle_small");
    public static final ResourceLocation Mend = texture("circle_mend");
    public static final float FormTicks = 14.0f;
    public static final float Margin = 1.12f;

    public DaeviteCircleRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
        shadowStrength = 0.0f;
    }

    private static ResourceLocation texture(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/daevite/" + Name + ".png");
    }

    @Override
    public void render(DaeviteCircle Circle, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Age = Circle.tickCount + Partial;
        float Grow = Mth.clamp(Age / FormTicks, 0.0f, 1.0f);
        Grow = 1.0f - (1.0f - Grow) * (1.0f - Grow) * (1.0f - Grow);
        float Fade = Circle.fade(Partial);
        if (Fade <= 0.0f || Grow <= 0.0f) {
            return;
        }
        float Radius = Circle.radius() * Margin * Grow;
        float Turn = Circle.getYRot();
        switch (Circle.kind()) {
            case DaeviteCircle.Binding -> {
                float Arm = Mth.clamp(Age / DaeviteCircle.BindArm, 0.0f, 1.0f);
                float Glow = (0.45f + 0.55f * Arm) * (0.85f + 0.15f * Mth.sin(Age * 0.9f));
                layer(Pose, Buffers, Bind, Radius, Turn + Age * 2.5f, 0.012f, Glow * Fade);
            }
            case DaeviteCircle.Ritual, DaeviteCircle.Summoning -> {
                float Power = Circle.power();
                float Pulse = 0.88f + 0.12f * Mth.sin(Age * (0.15f + Power * 0.25f));
                float Glow = (0.55f + 0.45f * Power) * Pulse * Fade;
                layer(Pose, Buffers, Ring, Radius, Turn + Age * (0.35f + Power * 1.2f), 0.012f, Glow);
                layer(Pose, Buffers, Sigil, Radius * 0.62f, Turn - Age * (0.7f + Power * 1.6f), 0.018f, Glow);
            }
            case DaeviteCircle.Channel -> layer(Pose, Buffers, Small, Radius, Turn + Age * 1.6f, 0.014f, (0.7f + 0.3f * Mth.sin(Age * 0.3f)) * Fade);
            default -> layer(Pose, Buffers, Mend, Radius, Turn + Age * 3.0f, 0.014f, 0.9f * Fade);
        }
        super.render(Circle, Yaw, Partial, Pose, Buffers, PackedLight);
    }

    private static void layer(PoseStack Pose, MultiBufferSource Buffers, ResourceLocation Texture, float Radius, float Spin, float Lift, float Glow) {
        boolean Additive = ShaderCompat.useFallback();
        RenderType Type = Additive ? RenderType.eyes(Texture) : RenderType.entityTranslucentEmissive(Texture);
        VertexConsumer Consumer = Buffers.getBuffer(Type);
        float Shade = Additive ? Glow : 1.0f;
        float Alpha = Additive ? 1.0f : Mth.clamp(Glow, 0.0f, 1.0f);
        Pose.pushPose();
        Pose.translate(0.0f, Lift, 0.0f);
        Pose.mulPose(Axis.YP.rotationDegrees(Spin));
        PoseStack.Pose Last = Pose.last();
        vertex(Consumer, Last, -Radius, -Radius, 0.0f, 0.0f, Shade, Alpha);
        vertex(Consumer, Last, -Radius, Radius, 0.0f, 1.0f, Shade, Alpha);
        vertex(Consumer, Last, Radius, Radius, 1.0f, 1.0f, Shade, Alpha);
        vertex(Consumer, Last, Radius, -Radius, 1.0f, 0.0f, Shade, Alpha);
        Pose.popPose();
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Z, float U, float V, float Shade, float Alpha) {
        Consumer.addVertex(Last, X, 0.0f, Z)
                .setColor(Shade, Shade, Shade, Alpha)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public boolean shouldRender(DaeviteCircle Circle, Frustum View, double CameraX, double CameraY, double CameraZ) {
        return View.isVisible(Circle.getBoundingBoxForCulling());
    }

    @Override
    public ResourceLocation getTextureLocation(DaeviteCircle Circle) {
        return Ring;
    }
}
