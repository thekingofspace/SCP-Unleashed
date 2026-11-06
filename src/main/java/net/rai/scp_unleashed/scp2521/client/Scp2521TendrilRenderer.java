package net.rai.scp_unleashed.scp2521.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp2521.Scp2521Registry;
import net.rai.scp_unleashed.scp2521.Scp2521Tendrils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Scp2521TendrilRenderer extends EntityRenderer<Scp2521Tendrils> {
    public static final ResourceLocation Texture = Scp2521Registry.location("textures/entity/scp2521/tendril.png");
    public static final int Count = 4;
    public static final int Segments = 26;
    public static final float Turns = 1.1f;
    public static final float Radius = 0.42f;
    public static final float Thick = 0.085f;
    public static final float Tip = 0.022f;
    public static final float Sink = 2.2f;

    public Scp2521TendrilRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(Scp2521Tendrils Owner) {
        return Texture;
    }

    @Override
    public void render(Scp2521Tendrils Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        float Grown = Owner.growth(PartialTick);
        if (Grown <= 0.01f) {
            return;
        }
        float Dragged = Owner.dragged(PartialTick);
        float Age = Owner.age(PartialTick);
        float Reach = Owner.reach();
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.entityCutoutNoCull(Texture));
        Pose.pushPose();
        Pose.translate(0.0f, -Sink * Dragged, 0.0f);
        PoseStack.Pose Last = Pose.last();
        for (int Strand = 0; Strand < Count; Strand++) {
            float Start = Strand * Mth.TWO_PI / Count + 0.4f;
            float Height = Reach * (0.62f + 0.09f * Strand);
            Vec3 Previous = null;
            int Drawn = Math.max(1, Math.round(Segments * Grown));
            for (int Step = 0; Step <= Drawn; Step++) {
                float Share = Step / (float) Segments;
                float Angle = Start + Turns * Mth.TWO_PI * Share + 0.25f * Mth.sin(Age * 3.0f + Strand + Share * 6.0f) - Dragged * 1.4f;
                float Spread = Radius * (1.0f - 0.32f * Share) + 0.15f * (float) Math.pow(1.0f - Share, 4) + 0.05f * (1.0f - Grown);
                Vec3 Point = new Vec3(Mth.sin(Angle) * Spread, Height * Share, Mth.cos(Angle) * Spread);
                if (Previous != null) {
                    float Width = Mth.lerp(Share, Thick, Tip);
                    prism(Buffer, Last, Previous, Point, Width, Light, Share);
                }
                Previous = Point;
            }
        }
        Pose.popPose();
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }

    private static void prism(VertexConsumer Buffer, PoseStack.Pose Last, Vec3 From, Vec3 To, float Width, int Light, float V) {
        Vec3 Axis = To.subtract(From);
        if (Axis.lengthSqr() < 1.0e-6) {
            return;
        }
        Vec3 Way = Axis.normalize();
        Vec3 Side = Way.cross(new Vec3(0.0, 1.0, 0.0));
        if (Side.lengthSqr() < 1.0e-4) {
            Side = new Vec3(1.0, 0.0, 0.0);
        }
        Side = Side.normalize().scale(Width);
        Vec3 Up = Way.cross(Side).normalize().scale(Width);
        Vec3[] Ring = {Side.add(Up), Side.subtract(Up), Side.scale(-1.0).subtract(Up), Side.scale(-1.0).add(Up)};
        Matrix4f Matrix = Last.pose();
        for (int Face = 0; Face < 4; Face++) {
            Vec3 A = Ring[Face];
            Vec3 B = Ring[(Face + 1) % 4];
            Vec3 Normal = A.add(B).normalize();
            float U0 = Face / 4.0f;
            float U1 = (Face + 1) / 4.0f;
            vertex(Buffer, Matrix, Last, From.add(A), U0, V, Normal, Light);
            vertex(Buffer, Matrix, Last, From.add(B), U1, V, Normal, Light);
            vertex(Buffer, Matrix, Last, To.add(B), U1, V + 0.04f, Normal, Light);
            vertex(Buffer, Matrix, Last, To.add(A), U0, V + 0.04f, Normal, Light);
        }
    }

    private static void vertex(VertexConsumer Buffer, Matrix4f Matrix, PoseStack.Pose Last, Vec3 Point, float U, float V, Vec3 Normal, int Light) {
        Vector3f Turned = Last.normal().transform(new Vector3f((float) Normal.x, (float) Normal.y, (float) Normal.z));
        Buffer.addVertex(Matrix, (float) Point.x, (float) Point.y, (float) Point.z).setColor(255, 255, 255, 255).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Light).setNormal(Turned.x(), Turned.y(), Turned.z());
    }
}
