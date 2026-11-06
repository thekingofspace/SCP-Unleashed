package net.rai.scp_unleashed.raid.hostile.client;

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
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.serpentshand.SerpentSeal;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class SerpentSealRenderer extends EntityRenderer<SerpentSeal> {
    public static final ResourceLocation Ring = HostileRegistry.location("textures/entity/serpents_hand/seal_ring.png");
    public static final ResourceLocation Sigil = HostileRegistry.location("textures/entity/serpents_hand/seal_sigil.png");
    public static final float Margin = 1.15f;
    public static final float FormTicks = 16.0f;

    public SerpentSealRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
        shadowStrength = 0.0f;
    }

    @Override
    public void render(SerpentSeal Seal, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Age = Seal.tickCount + Partial;
        float Glow = Seal.glow(Partial);
        if (Glow <= 0.01f) {
            return;
        }
        float Grow = Mth.clamp(Age / FormTicks, 0.0f, 1.0f);
        Grow = 1.0f - (1.0f - Grow) * (1.0f - Grow);
        if (Seal.closing()) {
            Grow = 0.6f + 0.4f * Glow;
        }
        float Radius = SerpentSeal.Radius * Margin * Grow;
        float Pulse = 0.86f + 0.14f * Mth.sin(Age * (Seal.flaring() ? 0.35f : 0.12f));
        float Turn = Seal.getYRot();
        layer(Pose, Buffers, Ring, Radius, Turn + Age * 0.6f, 0.012f, Glow * Pulse);
        layer(Pose, Buffers, Sigil, Radius * 0.58f, Turn - Age * 1.1f, 0.02f, Glow * Pulse);
        super.render(Seal, Yaw, Partial, Pose, Buffers, PackedLight);
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
    public boolean shouldRender(SerpentSeal Seal, Frustum View, double CameraX, double CameraY, double CameraZ) {
        return View.isVisible(Seal.getBoundingBoxForCulling());
    }

    @Override
    public ResourceLocation getTextureLocation(SerpentSeal Seal) {
        return Ring;
    }
}
