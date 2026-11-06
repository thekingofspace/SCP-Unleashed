package net.rai.scp_unleashed.scp2845.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp2845.Scp2845Comet;
import net.rai.scp_unleashed.scp2845.Scp2845Registry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class Scp2845CometRenderer extends EntityRenderer<Scp2845Comet> {
    public static final ResourceLocation GlowTexture = Scp2845Registry.location("textures/entity/scp2845/comet_glow.png");
    public static final ResourceLocation TailTexture = Scp2845Registry.location("textures/entity/scp2845/comet_tail.png");
    public static final float TailLength = 36.0f;
    public static final int TailSegments = 14;
    public static final double RenderReach = 640.0;

    public Scp2845CometRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public boolean shouldRender(Scp2845Comet Comet, Frustum View, double CameraX, double CameraY, double CameraZ) {
        if (Comet.getPhase() != Scp2845Comet.PhaseFlight || !Comet.isLaunched()) {
            return false;
        }
        Vec3 Head = Comet.getHead(0.0f);
        return Head.distanceToSqr(CameraX, CameraY, CameraZ) < RenderReach * RenderReach;
    }

    @Override
    public void render(Scp2845Comet Comet, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        float Progress = Comet.getProgress(PartialTick);
        if (Comet.getPhase() != Scp2845Comet.PhaseFlight || !Comet.isLaunched() || Progress >= 1.0f || ShaderCompat.renderingShadowPass()) {
            return;
        }
        Vec3 Origin = Comet.getPosition(PartialTick);
        Vec3 Head = Comet.getHead(PartialTick);
        Vec3 Camera = entityRenderDispatcher.camera.getPosition();
        Vec3 Back = Comet.getHeading().scale(-1.0);
        float Travelled = (float) Comet.getStart().distanceTo(Head);
        float Appear = Mth.clamp(Progress * 12.0f, 0.0f, 1.0f);
        float Time = Comet.tickCount + PartialTick;
        Pose.pushPose();
        Pose.translate(Head.x - Origin.x, Head.y - Origin.y, Head.z - Origin.z);
        VertexConsumer Tail = Buffers.getBuffer(RenderType.eyes(TailTexture));
        Vec3 ToCamera = Camera.subtract(Head);
        float Length = Math.min(TailLength, Travelled);
        ribbon(Pose, Tail, Back, ToCamera, Length, 2.2f, 0.4f, 0.55f, 1.0f, 0.78f, 1.2f, Appear);
        ribbon(Pose, Tail, Back, ToCamera, Length * 0.75f, 0.9f, 0.1f, 0.92f, 1.0f, 0.95f, 1.8f, Appear);
        Pose.pushPose();
        Pose.mulPose(entityRenderDispatcher.cameraOrientation());
        VertexConsumer Glow = Buffers.getBuffer(RenderType.eyes(GlowTexture));
        float Breath = 1.0f + 0.08f * Mth.sin(Time * 1.3f) + 0.05f * Mth.sin(Time * 3.7f);
        billboard(Pose, Glow, 6.0f * Breath, 0.45f * Appear, 1.0f * Appear, 0.8f * Appear);
        billboard(Pose, Glow, 2.6f * Breath, 0.95f * Appear, 1.0f * Appear, 0.96f * Appear);
        Pose.popPose();
        Pose.popPose();
    }

    private static void billboard(PoseStack Pose, VertexConsumer Buffer, float Size, float Red, float Green, float Blue) {
        PoseStack.Pose Last = Pose.last();
        float Half = Size * 0.5f;
        corner(Buffer, Last, -Half, -Half, 0.0f, 1.0f, Red, Green, Blue);
        corner(Buffer, Last, Half, -Half, 1.0f, 1.0f, Red, Green, Blue);
        corner(Buffer, Last, Half, Half, 1.0f, 0.0f, Red, Green, Blue);
        corner(Buffer, Last, -Half, Half, 0.0f, 0.0f, Red, Green, Blue);
    }

    private static void corner(VertexConsumer Buffer, PoseStack.Pose Last, float X, float Y, float U, float V, float Red, float Green, float Blue) {
        Buffer.addVertex(Last, X, Y, 0.0f).setColor(Red, Green, Blue, 1.0f).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(Last, 0.0f, 1.0f, 0.0f);
    }

    private static Vec3 side(Vec3 Along, Vec3 View) {
        Vec3 Across = Along.cross(View);
        if (Across.lengthSqr() < 1.0e-6) {
            Across = Along.cross(new Vec3(0.0, 1.0, 0.0));
        }
        if (Across.lengthSqr() < 1.0e-6) {
            Across = new Vec3(1.0, 0.0, 0.0);
        }
        return Across.normalize();
    }

    private static void ribbon(PoseStack Pose, VertexConsumer Buffer, Vec3 Back, Vec3 ToCamera, float Length, float HeadWidth, float TailWidth,
                               float Red, float Green, float Blue, float Falloff, float Appear) {
        if (Length < 0.5f) {
            return;
        }
        PoseStack.Pose Last = Pose.last();
        for (int Segment = 0; Segment < TailSegments; Segment++) {
            float Near = (float) Segment / TailSegments;
            float Far = (float) (Segment + 1) / TailSegments;
            Vec3 NearPoint = Back.scale(Near * Length);
            Vec3 FarPoint = Back.scale(Far * Length);
            Vec3 NearSide = side(Back, ToCamera.subtract(NearPoint)).scale(Mth.lerp(Near, HeadWidth, TailWidth) * 0.5f);
            Vec3 FarSide = side(Back, ToCamera.subtract(FarPoint)).scale(Mth.lerp(Far, HeadWidth, TailWidth) * 0.5f);
            float NearGlow = (float) Math.pow(1.0f - Near, Falloff) * Appear;
            float FarGlow = (float) Math.pow(1.0f - Far, Falloff) * Appear;
            Vec3[] Quad = {NearPoint.subtract(NearSide), NearPoint.add(NearSide), FarPoint.add(FarSide), FarPoint.subtract(FarSide)};
            float[] Glows = {NearGlow, NearGlow, FarGlow, FarGlow};
            float[] Us = {0.0f, 1.0f, 1.0f, 0.0f};
            float[] Vs = {Near, Near, Far, Far};
            for (int Index = 0; Index < 4; Index++) {
                vertex(Buffer, Last, Quad[Index], Us[Index], Vs[Index], Red * Glows[Index], Green * Glows[Index], Blue * Glows[Index]);
            }
            for (int Index = 3; Index >= 0; Index--) {
                vertex(Buffer, Last, Quad[Index], Us[Index], Vs[Index], Red * Glows[Index], Green * Glows[Index], Blue * Glows[Index]);
            }
        }
    }

    private static void vertex(VertexConsumer Buffer, PoseStack.Pose Last, Vec3 Point, float U, float V, float Red, float Green, float Blue) {
        Buffer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(Red, Green, Blue, 1.0f).setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(Last, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp2845Comet Comet) {
        return GlowTexture;
    }
}
