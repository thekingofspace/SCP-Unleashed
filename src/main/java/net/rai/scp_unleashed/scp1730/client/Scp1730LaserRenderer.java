package net.rai.scp_unleashed.scp1730.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.block.Scp1730LaserBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730LaserBlockEntity;
import org.joml.Matrix4f;

public class Scp1730LaserRenderer implements BlockEntityRenderer<Scp1730LaserBlockEntity> {
    public static final float Half = 0.03f;
    public static final float Dot = 0.07f;
    public static final float Halo = 0.16f;

    public Scp1730LaserRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp1730LaserBlockEntity Laser, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        if (!Laser.getBlockState().getValue(Scp1730LaserBlock.Powered) || Laser.getBlockState().getValue(Scp1730LaserBlock.Broken) || Laser.Beam <= 0.0) {
            return;
        }
        Direction Side = Laser.facing();
        Vec3 Lens = Laser.lens().subtract(Vec3.atLowerCornerOf(Laser.getBlockPos()));
        float Length = (float) Laser.Beam;
        float X0 = (float) Lens.x;
        float Y0 = (float) Lens.y;
        float Z0 = (float) Lens.z;
        float X1 = X0 + Side.getStepX() * Length;
        float Z1 = Z0 + Side.getStepZ() * Length;
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.lightning());
        Matrix4f Matrix = Pose.last().pose();
        float Pulse = 0.75f + 0.25f * (float) Math.sin((Laser.getLevel() == null ? 0 : Laser.getLevel().getGameTime() + Partial) * 0.4f);
        int Red = 255;
        int Green = (int) (40 * Pulse);
        int Alpha = (int) (200 * Pulse);
        boolean AlongX = Side.getAxis() == Direction.Axis.X;
        quad(Buffer, Matrix, X0, Y0 - Half, Z0, X1, Y0 + Half, Z1, AlongX, true, Red, Green, Alpha);
        quad(Buffer, Matrix, X0, Y0 - Half, Z0, X1, Y0 + Half, Z1, AlongX, false, Red, Green, Alpha);
        float Ex = X1 - Side.getStepX() * 0.004f;
        float Ez = Z1 - Side.getStepZ() * 0.004f;
        splash(Buffer, Matrix, Ex, Y0, Ez, AlongX, Halo, 255, (int) (30 * Pulse), (int) (90 * Pulse));
        splash(Buffer, Matrix, Ex, Y0, Ez, AlongX, Dot, 255, (int) (150 + 60 * Pulse), (int) (230 * Pulse));
        float Bx = X1 - Side.getStepX() * Dot;
        float Bz = Z1 - Side.getStepZ() * Dot;
        quad(Buffer, Matrix, Bx, Y0 - Dot * 0.6f, Bz, X1, Y0 + Dot * 0.6f, Z1, AlongX, true, 255, (int) (120 * Pulse), (int) (200 * Pulse));
        quad(Buffer, Matrix, Bx, Y0 - Dot * 0.6f, Bz, X1, Y0 + Dot * 0.6f, Z1, AlongX, false, 255, (int) (120 * Pulse), (int) (200 * Pulse));
    }

    private static void splash(VertexConsumer Buffer, Matrix4f Matrix, float X, float Y, float Z, boolean AlongX, float Size, int Red, int Green, int Alpha) {
        float Dx = AlongX ? 0.0f : Size;
        float Dz = AlongX ? Size : 0.0f;
        Buffer.addVertex(Matrix, X - Dx, Y - Size, Z - Dz).setColor(Red, Green, Green, Alpha);
        Buffer.addVertex(Matrix, X + Dx, Y - Size, Z + Dz).setColor(Red, Green, Green, Alpha);
        Buffer.addVertex(Matrix, X + Dx, Y + Size, Z + Dz).setColor(Red, Green, Green, Alpha);
        Buffer.addVertex(Matrix, X - Dx, Y + Size, Z - Dz).setColor(Red, Green, Green, Alpha);
        Buffer.addVertex(Matrix, X - Dx, Y + Size, Z - Dz).setColor(Red, Green, Green, Alpha);
        Buffer.addVertex(Matrix, X + Dx, Y + Size, Z + Dz).setColor(Red, Green, Green, Alpha);
        Buffer.addVertex(Matrix, X + Dx, Y - Size, Z + Dz).setColor(Red, Green, Green, Alpha);
        Buffer.addVertex(Matrix, X - Dx, Y - Size, Z - Dz).setColor(Red, Green, Green, Alpha);
    }

    private static void quad(VertexConsumer Buffer, Matrix4f Matrix, float X0, float Y0, float Z0, float X1, float Y1, float Z1, boolean AlongX, boolean Vertical, int Red,
                             int Green, int Alpha) {
        float[][] Corners;
        if (Vertical) {
            Corners = new float[][]{{X0, Y0, Z0}, {X1, Y0, Z1}, {X1, Y1, Z1}, {X0, Y1, Z0}};
        } else {
            float Mid = (Y0 + Y1) * 0.5f;
            float Width = (Y1 - Y0) * 0.5f;
            float Dx = AlongX ? 0.0f : Width;
            float Dz = AlongX ? Width : 0.0f;
            Corners = new float[][]{{X0 - Dx, Mid, Z0 - Dz}, {X1 - Dx, Mid, Z1 - Dz}, {X1 + Dx, Mid, Z1 + Dz}, {X0 + Dx, Mid, Z0 + Dz}};
        }
        for (int Index = 0; Index < 4; Index++) {
            Buffer.addVertex(Matrix, Corners[Index][0], Corners[Index][1], Corners[Index][2]).setColor(Red, Green, Green, Alpha);
        }
        for (int Index = 3; Index >= 0; Index--) {
            Buffer.addVertex(Matrix, Corners[Index][0], Corners[Index][1], Corners[Index][2]).setColor(Red, Green, Green, Alpha);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(Scp1730LaserBlockEntity Laser) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(Scp1730LaserBlockEntity Laser) {
        return new AABB(Laser.getBlockPos()).inflate(Scp1730LaserBlockEntity.Reach + 1.0, 1.0, Scp1730LaserBlockEntity.Reach + 1.0);
    }
}
