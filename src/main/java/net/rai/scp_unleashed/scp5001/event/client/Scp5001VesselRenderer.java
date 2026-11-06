package net.rai.scp_unleashed.scp5001.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Vessel;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp5001VesselRenderer extends GeoEntityRenderer<Scp5001Vessel> {
    public static final float Scale = 1.5f;
    public static final float WaveSpeed = 0.85f;
    public static final int WaveLife = 150;
    public static final int Echoes = 3;
    public static final int EchoGap = 7;
    public static final double CullReach = 220.0;

    public Scp5001VesselRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp5001VesselModel());
        withScale(Scale);
        shadowRadius = 0.0f;
        addRenderLayer(new Scp5001GlowLayer<>(this, Owner -> Scp5001VesselModel.GlowPath, Owner -> breath(Owner)));
    }

    public static double breath(Scp5001Vessel Owner) {
        float Time = Owner.tickCount;
        float Speed = switch (Owner.mode()) {
            case Scp5001Vessel.ModeStrain -> 0.35f;
            case Scp5001Vessel.ModePinned -> 0.6f;
            case Scp5001Vessel.ModeRise -> 0.18f;
            default -> 0.06f;
        };
        float Floor = Owner.mode() == Scp5001Vessel.ModeIdle ? 0.55f : 0.7f;
        return Floor + (1.0f - Floor) * (0.5f + 0.5f * Mth.sin(Time * Speed));
    }

    @Override
    public boolean shouldRender(Scp5001Vessel Owner, Frustum View, double CameraX, double CameraY, double CameraZ) {
        return Owner.distanceToSqr(CameraX, CameraY, CameraZ) < CullReach * CullReach;
    }

    @Override
    public void render(Scp5001Vessel Owner, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        super.render(Owner, Yaw, Partial, Pose, Buffers, Light);
        rings(Owner, Partial, Pose, Buffers);
    }

    private static void rings(Scp5001Vessel Owner, float Partial, PoseStack Pose, MultiBufferSource Buffers) {
        float Age = Owner.tickCount - Owner.WaveStart + Partial;
        if (Age < 0.0f || Age > WaveLife + Echoes * EchoGap) {
            return;
        }
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.lightning());
        Matrix4f Matrix = Pose.last().pose();
        for (int Echo = 0; Echo < Echoes; Echo++) {
            float Own = Age - Echo * EchoGap;
            if (Own <= 0.0f || Own > WaveLife) {
                continue;
            }
            float Radius = 2.5f + Own * WaveSpeed;
            float Fade = 1.0f - Own / WaveLife;
            int Alpha = (int) (150 * Fade * (Echo == 0 ? 1.0f : 0.5f));
            float Centre = (float) Scp5001Vessel.CentreHeight;
            band(Matrix, Buffer, Radius, Centre - 0.45f, Centre + 0.45f, 156, 140, 240, Alpha);
            band(Matrix, Buffer, Radius * 0.985f, Centre - 0.15f, Centre + 0.15f, 220, 214, 255, Math.min(255, Alpha + 40));
            band(Matrix, Buffer, Radius * 0.92f, Centre - 5.5f, Centre - 5.2f, 156, 140, 240, Alpha / 3);
            band(Matrix, Buffer, Radius * 0.92f, Centre + 5.2f, Centre + 5.5f, 156, 140, 240, Alpha / 3);
        }
    }

    private static void band(Matrix4f Matrix, VertexConsumer Buffer, float Radius, float Low, float High, int Red, int Green, int Blue, int Alpha) {
        if (Alpha <= 0) {
            return;
        }
        for (int Index = 0; Index < 8; Index++) {
            float A = (Index + 0.5f) * Mth.TWO_PI / 8.0f;
            float B = (Index + 1.5f) * Mth.TWO_PI / 8.0f;
            float Ax = Mth.cos(A) * Radius;
            float Az = Mth.sin(A) * Radius;
            float Bx = Mth.cos(B) * Radius;
            float Bz = Mth.sin(B) * Radius;
            Buffer.addVertex(Matrix, Ax, Low, Az).setColor(Red, Green, Blue, Alpha);
            Buffer.addVertex(Matrix, Bx, Low, Bz).setColor(Red, Green, Blue, Alpha);
            Buffer.addVertex(Matrix, Bx, High, Bz).setColor(Red, Green, Blue, Alpha);
            Buffer.addVertex(Matrix, Ax, High, Az).setColor(Red, Green, Blue, Alpha);
            Buffer.addVertex(Matrix, Ax, High, Az).setColor(Red, Green, Blue, Alpha);
            Buffer.addVertex(Matrix, Bx, High, Bz).setColor(Red, Green, Blue, Alpha);
            Buffer.addVertex(Matrix, Bx, Low, Bz).setColor(Red, Green, Blue, Alpha);
            Buffer.addVertex(Matrix, Ax, Low, Az).setColor(Red, Green, Blue, Alpha);
        }
    }
}
