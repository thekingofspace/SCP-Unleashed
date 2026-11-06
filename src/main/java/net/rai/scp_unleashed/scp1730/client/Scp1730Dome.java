package net.rai.scp_unleashed.scp1730.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class Scp1730Dome {
    public static final int Around = 32;
    public static final int Up = 24;
    public static final float BandHalf = 0.18f;
    public static final float GlintWidth = 0.22f;

    private Scp1730Dome() {
    }

    public static void render(PoseStack Pose, MultiBufferSource Buffers, double Radius, float Alpha, float Spin) {
        if (Alpha <= 0.002f || Radius <= 0.01) {
            return;
        }
        VertexConsumer Buffer = Buffers.getBuffer(Scp1730RenderTypes.Glow);
        Matrix4f Matrix = Pose.last().pose();
        double Glint = Math.sin(Spin * 12.0) * Math.PI * 0.5;
        for (int Ring = 0; Ring < Up; Ring++) {
            double Low = (Ring / (double) Up - 0.5) * Math.PI;
            double High = ((Ring + 1) / (double) Up - 0.5) * Math.PI;
            double Middle = (Low + High) * 0.5;
            float Band = 0.6f + 0.4f * (Ring % 2);
            float Sweep = 1.0f + 1.4f * (float) Math.exp(-Math.pow((Middle - Glint) / GlintWidth, 2.0));
            for (int Step = 0; Step < Around; Step++) {
                double Left = Step / (double) Around * Math.PI * 2.0 + Spin;
                double Right = (Step + 1) / (double) Around * Math.PI * 2.0 + Spin;
                float Shimmer = 0.8f + 0.2f * Mth.sin((float) (Step * 0.9 + Ring * 1.7) + Spin * 40.0f);
                int A = Mth.clamp((int) (Alpha * Band * Sweep * Shimmer * 255.0f), 0, 255);
                vertex(Buffer, Matrix, Radius, Low, Left, A);
                vertex(Buffer, Matrix, Radius, Low, Right, A);
                vertex(Buffer, Matrix, Radius, High, Right, A);
                vertex(Buffer, Matrix, Radius, High, Left, A);
            }
        }
        rim(Buffer, Matrix, Radius, Alpha, Spin);
    }

    private static void rim(VertexConsumer Buffer, Matrix4f Matrix, double Radius, float Alpha, float Spin) {
        float Edge = (float) (Radius * 1.004);
        for (int Step = 0; Step < Around * 2; Step++) {
            double Left = Step / (double) (Around * 2) * Math.PI * 2.0;
            double Right = (Step + 1) / (double) (Around * 2) * Math.PI * 2.0;
            float Wave = 0.5f + 0.5f * Mth.sin((float) (Left * 6.0) - Spin * 30.0f);
            int A = Mth.clamp((int) (Alpha * (2.2f + 2.6f * Wave) * 255.0f), 0, 255);
            float X0 = (float) (Math.cos(Left) * Edge);
            float Z0 = (float) (Math.sin(Left) * Edge);
            float X1 = (float) (Math.cos(Right) * Edge);
            float Z1 = (float) (Math.sin(Right) * Edge);
            Buffer.addVertex(Matrix, X0, -BandHalf, Z0).setColor(170, 230, 255, 0);
            Buffer.addVertex(Matrix, X1, -BandHalf, Z1).setColor(170, 230, 255, 0);
            Buffer.addVertex(Matrix, X1, 0.0f, Z1).setColor(170, 230, 255, A);
            Buffer.addVertex(Matrix, X0, 0.0f, Z0).setColor(170, 230, 255, A);
            Buffer.addVertex(Matrix, X0, 0.0f, Z0).setColor(170, 230, 255, A);
            Buffer.addVertex(Matrix, X1, 0.0f, Z1).setColor(170, 230, 255, A);
            Buffer.addVertex(Matrix, X1, BandHalf, Z1).setColor(170, 230, 255, 0);
            Buffer.addVertex(Matrix, X0, BandHalf, Z0).setColor(170, 230, 255, 0);
        }
    }

    private static void vertex(VertexConsumer Buffer, Matrix4f Matrix, double Radius, double Lift, double Turn, int Alpha) {
        float X = (float) (Math.cos(Lift) * Math.cos(Turn) * Radius);
        float Y = (float) (Math.sin(Lift) * Radius);
        float Z = (float) (Math.cos(Lift) * Math.sin(Turn) * Radius);
        Buffer.addVertex(Matrix, X, Y, Z).setColor(120, 190, 255, Alpha);
    }
}
