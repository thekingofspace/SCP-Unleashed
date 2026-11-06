package net.rai.scp_unleashed.scp002.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
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
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp002.MeteorEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MeteorRenderer extends EntityRenderer<MeteorEntity> {
    public static final ResourceLocation RockTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp002/meteor.png");
    public static final ResourceLocation CrackTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp002/meteor_cracks.png");
    public static final ResourceLocation GlowTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp002/meteor_glow.png");
    public static final ResourceLocation TailTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp002/meteor_tail.png");
    public static final float RockScale = 2.4f;
    public static final float TailLength = 32.0f;
    public static final int TailSegments = 14;
    public static final double RenderReach = 640.0;

    private final ModelPart Rock;

    public MeteorRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.Rock = Context.bakeLayer(Scp002ClientEvents.MeteorLayer);
        this.shadowRadius = 0.0f;
    }

    public static LayerDefinition createRock() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("rock", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0f, -6.0f, -6.0f, 12.0f, 12.0f, 12.0f)
                .texOffs(0, 24).addBox(-4.0f, -8.0f, -5.0f, 8.0f, 2.0f, 9.0f)
                .texOffs(0, 35).addBox(-5.0f, 6.0f, -3.0f, 9.0f, 2.0f, 7.0f)
                .texOffs(34, 24).addBox(6.0f, -4.0f, -4.0f, 2.0f, 7.0f, 8.0f)
                .texOffs(34, 39).addBox(-8.0f, -3.0f, -3.0f, 2.0f, 6.0f, 7.0f)
                .texOffs(0, 44).addBox(-3.0f, -4.0f, 6.0f, 7.0f, 7.0f, 2.0f)
                .texOffs(0, 53).addBox(-4.0f, -3.0f, -8.0f, 7.0f, 6.0f, 2.0f), PartPose.ZERO);
        return LayerDefinition.create(Mesh, 64, 64);
    }

    @Override
    public boolean shouldRender(MeteorEntity Meteor, Frustum View, double CameraX, double CameraY, double CameraZ) {
        if (Meteor.getPhase() != MeteorEntity.PhaseFlight || !Meteor.isLaunched()) {
            return false;
        }
        Vec3 Head = Meteor.getHead(0.0f);
        double Dx = Head.x - CameraX;
        double Dy = Head.y - CameraY;
        double Dz = Head.z - CameraZ;
        return Dx * Dx + Dy * Dy + Dz * Dz < RenderReach * RenderReach;
    }

    @Override
    public void render(MeteorEntity Meteor, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        float Progress = Meteor.getProgress(PartialTick);
        if (Meteor.getPhase() != MeteorEntity.PhaseFlight || !Meteor.isLaunched() || Progress >= 1.0f) {
            return;
        }
        Vec3 Origin = Meteor.getPosition(PartialTick);
        Vec3 Head = Meteor.getHead(PartialTick);
        Vec3 Camera = entityRenderDispatcher.camera.getPosition();
        Vec3 Back = Meteor.getHeading().scale(-1.0);
        float Travelled = (float) Meteor.getStart().distanceTo(Head);
        float Appear = Mth.clamp(Progress * 14.0f, 0.0f, 1.0f);
        float Time = Meteor.tickCount + PartialTick;
        Pose.pushPose();
        Pose.translate(Head.x - Origin.x, Head.y - Origin.y, Head.z - Origin.z);
        boolean ShadowPass = ShaderCompat.renderingShadowPass();
        if (!ShadowPass) {
            VertexConsumer Tail = Buffers.getBuffer(RenderType.eyes(TailTexture));
            Vec3 ToCamera = Camera.subtract(Head);
            float Length = Math.min(TailLength, Travelled);
            ribbon(Pose, Tail, Back, ToCamera, Length, 1.9f, 0.5f, 1.0f, 0.42f, 0.1f, 1.3f, Appear);
            ribbon(Pose, Tail, Back, ToCamera, Length * 0.7f, 0.8f, 0.12f, 1.0f, 0.86f, 0.55f, 2.0f, Appear);
        }
        drawRock(Pose, Buffers, Meteor, Time, ShadowPass);
        if (!ShadowPass) {
            Pose.pushPose();
            Pose.mulPose(entityRenderDispatcher.cameraOrientation());
            VertexConsumer Glow = Buffers.getBuffer(RenderType.eyes(GlowTexture));
            float Breath = 1.0f + 0.07f * Mth.sin(Time * 1.7f) + 0.04f * Mth.sin(Time * 4.3f);
            billboard(Pose, Glow, 5.2f * Breath, 1.0f * Appear, 0.5f * Appear, 0.18f * Appear);
            billboard(Pose, Glow, 2.3f * Breath, 1.0f * Appear, 0.88f * Appear, 0.66f * Appear);
            Pose.popPose();
        }
        Pose.popPose();
    }

    private void drawRock(PoseStack Pose, MultiBufferSource Buffers, MeteorEntity Meteor, float Time, boolean ShadowPass) {
        Pose.pushPose();
        int Seed = Meteor.getSeed();
        Vector3f Axis = new Vector3f(Mth.sin(Seed * 0.37f), 0.6f, Mth.cos(Seed * 0.53f)).normalize();
        Pose.mulPose(new Quaternionf().rotateAxis(Time * 0.12f + Seed, Axis));
        Pose.scale(RockScale, RockScale, RockScale);
        Rock.render(Pose, Buffers.getBuffer(RenderType.entityCutoutNoCull(RockTexture)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        if (!ShadowPass) {
            float Heat = 0.8f + 0.2f * Mth.sin(Time * 0.9f);
            int Shade = (int) (Heat * 255.0f);
            int Colour = 0xFF000000 | Shade << 16 | Shade << 8 | Shade;
            Rock.render(Pose, Buffers.getBuffer(RenderType.eyes(CrackTexture)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Colour);
        }
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
                trailVertex(Buffer, Last, Quad[Index], Us[Index], Vs[Index], Red * Glows[Index], Green * Glows[Index], Blue * Glows[Index]);
            }
            for (int Index = 3; Index >= 0; Index--) {
                trailVertex(Buffer, Last, Quad[Index], Us[Index], Vs[Index], Red * Glows[Index], Green * Glows[Index], Blue * Glows[Index]);
            }
        }
    }

    private static void trailVertex(VertexConsumer Buffer, PoseStack.Pose Last, Vec3 Point, float U, float V, float Red, float Green, float Blue) {
        Buffer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(Red, Green, Blue, 1.0f).setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(Last, 0.0f, 1.0f, 0.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity Meteor) {
        return RockTexture;
    }
}
