package net.rai.scp_unleashed.scp001.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rifttech.client.Ribbons;
import net.rai.scp_unleashed.rifttech.client.RiftTechDeferred;
import net.rai.scp_unleashed.rifttech.client.RiftTechRenderTypes;
import net.rai.scp_unleashed.scp001.Scp001Registry;
import net.rai.scp_unleashed.scp001.Scp001Singularity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class Scp001SingularityRenderer extends EntityRenderer<Scp001Singularity> {
    public static final ResourceLocation CoreTexture = Scp001Registry.location("textures/entity/scp001/singularity_core.png");
    public static final ResourceLocation HaloTexture = Scp001Registry.location("textures/entity/scp001/singularity_halo.png");
    public static final float CoreRadius = 0.17f;
    public static final float RingInner = 0.23f;
    public static final float RingOuter = 0.3f;
    public static final float HaloRadius = 0.62f;
    public static final float RingTilt = 68.0f;
    public static final int Slices = 14;
    public static final int Stacks = 8;
    public static final int Segments = 28;
    private static final float[] Gold = {1.0f, 0.76f, 0.24f};
    private static final float[] Hot = {1.0f, 0.95f, 0.72f};

    public Scp001SingularityRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public void render(Scp001Singularity Orb, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        float Size = Orb.size(PartialTick);
        if (Size <= 0.01f) {
            return;
        }
        float Time = Orb.tickCount + PartialTick;
        Pose.pushPose();
        Pose.translate(0.0, Orb.getBbHeight() * 0.5, 0.0);
        Pose.pushPose();
        Pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float Halo = HaloRadius * Size * (1.0f + 0.07f * Mth.sin(Time * 0.45f));
        halo(Buffers.getBuffer(RenderType.entityTranslucent(HaloTexture)), Pose.last(), Halo, Time);
        Pose.popPose();
        sphere(Buffers.getBuffer(RenderType.entityCutoutNoCull(CoreTexture)), Pose.last(), CoreRadius * Size);
        if (!ShaderCompat.renderingShadowPass()) {
            Pose.pushPose();
            Pose.mulPose(Axis.YP.rotationDegrees(Time * 9.0f));
            Pose.mulPose(Axis.XP.rotationDegrees(RingTilt + 6.0f * Mth.sin(Time * 0.21f)));
            PoseStack.Pose Saved = Pose.last().copy();
            Pose.popPose();
            float Heat = (0.9f + 0.25f * Mth.sin(Time * 0.7f)) * (Orb.flying() ? 1.15f : 1.0f);
            float Inner = RingInner * Size;
            float Outer = RingOuter * Size;
            float Spin = Time * 0.35f;
            RiftTechDeferred.queue(Own -> ring(Own.getBuffer(RiftTechRenderTypes.Arc), Saved, Inner, Outer, Spin, Heat));
        }
        Pose.popPose();
        super.render(Orb, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    private static void halo(VertexConsumer Consumer, PoseStack.Pose Last, float Radius, float Time) {
        Vec3 Facing = new Vec3(0.0, 0.0, 1.0);
        float Alpha = 0.55f + 0.1f * Mth.sin(Time * 0.3f);
        Ribbons.vertex(Consumer, Last, new Vec3(-Radius, -Radius, 0.0), 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, Alpha, Facing);
        Ribbons.vertex(Consumer, Last, new Vec3(Radius, -Radius, 0.0), 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, Alpha, Facing);
        Ribbons.vertex(Consumer, Last, new Vec3(Radius, Radius, 0.0), 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, Alpha, Facing);
        Ribbons.vertex(Consumer, Last, new Vec3(-Radius, Radius, 0.0), 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, Alpha, Facing);
    }

    private static void sphere(VertexConsumer Consumer, PoseStack.Pose Last, float Radius) {
        for (int Stack = 0; Stack < Stacks; Stack++) {
            float Top = Mth.PI * Stack / Stacks;
            float Bottom = Mth.PI * (Stack + 1) / Stacks;
            for (int Slice = 0; Slice < Slices; Slice++) {
                float Left = Mth.TWO_PI * Slice / Slices;
                float Right = Mth.TWO_PI * (Slice + 1) / Slices;
                Vec3 A = point(Top, Left);
                Vec3 B = point(Bottom, Left);
                Vec3 C = point(Bottom, Right);
                Vec3 D = point(Top, Right);
                float U0 = (float) Slice / Slices;
                float U1 = (float) (Slice + 1) / Slices;
                float V0 = (float) Stack / Stacks;
                float V1 = (float) (Stack + 1) / Stacks;
                corner(Consumer, Last, A, Radius, U0, V0);
                corner(Consumer, Last, B, Radius, U0, V1);
                corner(Consumer, Last, C, Radius, U1, V1);
                corner(Consumer, Last, D, Radius, U1, V0);
            }
        }
    }

    private static Vec3 point(float Polar, float Azimuth) {
        return new Vec3(Mth.sin(Polar) * Mth.cos(Azimuth), Mth.cos(Polar), Mth.sin(Polar) * Mth.sin(Azimuth));
    }

    private static void corner(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Normal, float Radius, float U, float V) {
        Ribbons.vertex(Consumer, Last, Normal.scale(Radius), U, V, 1.0f, 1.0f, 1.0f, 1.0f, Normal);
    }

    private static void ring(VertexConsumer Consumer, PoseStack.Pose Last, float Inner, float Outer, float Spin, float Heat) {
        Vec3 Up = new Vec3(0.0, 1.0, 0.0);
        for (int Segment = 0; Segment < Segments; Segment++) {
            float From = Mth.TWO_PI * Segment / Segments + Spin;
            float To = Mth.TWO_PI * (Segment + 1) / Segments + Spin;
            float Flicker = Heat * (0.75f + 0.25f * Mth.sin(Segment * 1.7f + Spin * 3.0f));
            Vec3 InnerFrom = new Vec3(Mth.cos(From) * Inner, 0.0, Mth.sin(From) * Inner);
            Vec3 OuterFrom = new Vec3(Mth.cos(From) * Outer, 0.0, Mth.sin(From) * Outer);
            Vec3 InnerTo = new Vec3(Mth.cos(To) * Inner, 0.0, Mth.sin(To) * Inner);
            Vec3 OuterTo = new Vec3(Mth.cos(To) * Outer, 0.0, Mth.sin(To) * Outer);
            Ribbons.vertex(Consumer, Last, InnerFrom, 0.0f, 0.5f, Hot[0] * Flicker, Hot[1] * Flicker, Hot[2] * Flicker, 1.0f, Up);
            Ribbons.vertex(Consumer, Last, OuterFrom, 1.0f, 0.5f, Gold[0] * Flicker, Gold[1] * Flicker, Gold[2] * Flicker, 1.0f, Up);
            Ribbons.vertex(Consumer, Last, OuterTo, 1.0f, 0.5f, Gold[0] * Flicker, Gold[1] * Flicker, Gold[2] * Flicker, 1.0f, Up);
            Ribbons.vertex(Consumer, Last, InnerTo, 0.0f, 0.5f, Hot[0] * Flicker, Hot[1] * Flicker, Hot[2] * Flicker, 1.0f, Up);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(Scp001Singularity Orb) {
        return CoreTexture;
    }
}
