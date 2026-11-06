package net.rai.scp_unleashed.scp163.client;

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
import net.rai.scp_unleashed.scp163.Scp163CraftEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Scp163CraftRenderer extends EntityRenderer<Scp163CraftEntity> {
    public static final ResourceLocation HullTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp163/craft.png");
    public static final ResourceLocation HeatTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp163/craft_heat.png");
    public static final ResourceLocation TailTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp163/craft_tail.png");
    public static final float CraftScale = 6.0f;
    public static final float TailLength = 36.0f;
    public static final int TailSegments = 14;
    public static final double RenderReach = 640.0;

    private final ModelPart Hull;

    public Scp163CraftRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.Hull = Context.bakeLayer(Scp163ClientEvents.CraftLayer);
        this.shadowRadius = 0.0f;
    }

    public static LayerDefinition createHull() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("hull", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0f, -4.0f, -12.0f, 12.0f, 8.0f, 24.0f)
                .texOffs(72, 0).addBox(-4.0f, -3.0f, -16.0f, 8.0f, 6.0f, 4.0f)
                .texOffs(72, 10).addBox(-4.0f, -3.0f, 12.0f, 8.0f, 6.0f, 3.0f)
                .texOffs(0, 32).addBox(-2.0f, 4.0f, -8.0f, 4.0f, 2.0f, 16.0f)
                .texOffs(40, 32).addBox(-3.0f, -6.0f, -6.0f, 6.0f, 2.0f, 12.0f), PartPose.ZERO);
        return LayerDefinition.create(Mesh, 128, 64);
    }

    @Override
    public boolean shouldRender(Scp163CraftEntity Craft, Frustum View, double CameraX, double CameraY, double CameraZ) {
        if (Craft.getPhase() != Scp163CraftEntity.PhaseFlight || !Craft.isLaunched()) {
            return false;
        }
        Vec3 Head = Craft.getHead(0.0f);
        double Dx = Head.x - CameraX;
        double Dy = Head.y - CameraY;
        double Dz = Head.z - CameraZ;
        return Dx * Dx + Dy * Dy + Dz * Dz < RenderReach * RenderReach;
    }

    @Override
    public void render(Scp163CraftEntity Craft, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        float Progress = Craft.getProgress(PartialTick);
        if (Craft.getPhase() != Scp163CraftEntity.PhaseFlight || !Craft.isLaunched() || Progress >= 1.0f) {
            return;
        }
        Vec3 Origin = Craft.getPosition(PartialTick);
        Vec3 Head = Craft.getHead(PartialTick);
        Vec3 Camera = entityRenderDispatcher.camera.getPosition();
        Vec3 Heading = Craft.getHeading();
        Vec3 Back = Heading.scale(-1.0);
        float Travelled = (float) Craft.getStart().distanceTo(Head);
        float Appear = Mth.clamp(Progress * 12.0f, 0.0f, 1.0f);
        float Time = Craft.tickCount + PartialTick;
        boolean ShadowPass = ShaderCompat.renderingShadowPass();
        Pose.pushPose();
        Pose.translate(Head.x - Origin.x, Head.y - Origin.y, Head.z - Origin.z);
        if (!ShadowPass) {
            VertexConsumer Tail = Buffers.getBuffer(RenderType.eyes(TailTexture));
            Vec3 ToCamera = Camera.subtract(Head);
            float Length = Math.min(TailLength, Travelled);
            ribbon(Pose, Tail, Back, ToCamera, Length, 4.2f, 1.2f, 1.0f, 0.55f, 0.2f, 1.4f, Appear);
            ribbon(Pose, Tail, Back, ToCamera, Length * 0.6f, 1.8f, 0.4f, 0.75f, 0.95f, 1.0f, 2.0f, Appear);
        }
        Pose.pushPose();
        float Turn = (float) Math.atan2(Heading.x, Heading.z);
        float Dip = (float) Math.asin(Mth.clamp(-Heading.y, -1.0, 1.0));
        Pose.mulPose(new Quaternionf().rotateY(Turn + (float) Math.PI));
        Pose.mulPose(new Quaternionf().rotateX(-Dip));
        Pose.mulPose(new Quaternionf().rotateZ(0.08f * Mth.sin(Time * 0.21f + Craft.getSeed())));
        Pose.scale(CraftScale, CraftScale, CraftScale);
        Hull.render(Pose, Buffers.getBuffer(RenderType.entityCutoutNoCull(HullTexture)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        if (!ShadowPass) {
            float Heat = 0.75f + 0.25f * Mth.sin(Time * 1.3f);
            int Shade = (int) (Heat * 255.0f);
            int Colour = 0xFF000000 | Shade << 16 | Shade << 8 | Shade;
            Hull.render(Pose, Buffers.getBuffer(RenderType.eyes(HeatTexture)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Colour);
        }
        Pose.popPose();
        Pose.popPose();
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
    public ResourceLocation getTextureLocation(Scp163CraftEntity Craft) {
        return HullTexture;
    }
}
