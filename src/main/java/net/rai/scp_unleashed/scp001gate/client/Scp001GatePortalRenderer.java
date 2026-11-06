package net.rai.scp_unleashed.scp001gate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001gate.Scp001GatePortalBlockEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class Scp001GatePortalRenderer implements BlockEntityRenderer<Scp001GatePortalBlockEntity> {
    public static final double ViewDepth = 24.0;
    public static final double ViewWidth = 64.0;
    public static final double ViewTop = 22.0;
    public static final double ViewSpan = 24.0;
    public static final double Margin = 1.25;
    public static final int Split = 2;

    public Scp001GatePortalRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static double[] panel(double Px, double Py, Vec3 Eye) {
        double Rx = Px - Eye.x;
        double Ry = Py - Eye.y;
        double Rz = -Eye.z;
        boolean Front = Rz < 0.0;
        double Reach = ViewDepth / Math.max(1.0e-4, Math.abs(Rz));
        double Hx = Px + Rx * Reach;
        double Hy = Py + Ry * Reach;
        double Across = Front ? Hx : -Hx;
        double U = 0.5 + Across / ViewWidth + (Front ? 0.0 : 0.5);
        double V = Math.min(0.995, Math.max(0.005, (ViewTop - Hy) / ViewSpan));
        return new double[]{U, V};
    }

    @Override
    public void render(Scp001GatePortalBlockEntity Portal, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        if (ShaderCompat.renderingShadowPass()) {
            return;
        }
        BlockPos Pos = Portal.getBlockPos();
        BlockPos Centre = Portal.centre();
        Direction Front = Portal.front();
        Direction Right = Front.getClockWise();
        Vec3 Camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double Cx = Centre.getX() + 0.5;
        double Cz = Centre.getZ() + 0.5;
        Vec3 RightAxis = new Vec3(Right.getStepX(), 0.0, Right.getStepZ());
        Vec3 FrontAxis = new Vec3(Front.getStepX(), 0.0, Front.getStepZ());
        Vec3 Offset = new Vec3(Camera.x - Cx, Camera.y - Centre.getY(), Camera.z - Cz);
        Vec3 Eye = new Vec3(Offset.dot(RightAxis), Offset.y, Offset.dot(FrontAxis));
        Vec3 Middle = new Vec3(Pos.getX() + 0.5 - Cx, 0.0, Pos.getZ() + 0.5 - Cz);
        double Across = Middle.dot(RightAxis);
        double Low = Pos.getY() - Centre.getY();
        net.minecraft.resources.ResourceLocation View = Portal.leadsHome() ? Scp001GateLight.Plain : Scp001GateLight.Garden;
        RenderType Kind = ShaderCompat.useFallback() ? RenderType.entityTranslucentEmissive(View) : RenderType.entityCutoutNoCull(View);
        VertexConsumer Consumer = Buffers.getBuffer(Kind);
        PoseStack.Pose Last = Pose.last();
        double Step = 1.0 / Split;
        for (int Sx = 0; Sx < Split; Sx++) {
            for (int Sy = 0; Sy < Split; Sy++) {
                double X0 = Across - 0.5 + Sx * Step;
                double X1 = X0 + Step;
                double Y0 = Low + Sy * Step;
                double Y1 = Y0 + Step;
                double[][] Corners = {{X0, Y1}, {X0, Y0}, {X1, Y0}, {X1, Y1}};
                double[][] Uvs = new double[4][];
                double Least = Double.MAX_VALUE;
                for (int Index = 0; Index < 4; Index++) {
                    Uvs[Index] = panel(Corners[Index][0], Corners[Index][1], Eye);
                    Least = Math.min(Least, Uvs[Index][0]);
                }
                double Base = Math.floor(Least);
                double Most = 0.0;
                for (double[] Each : Uvs) {
                    Each[0] -= Base;
                    Most = Math.max(Most, Each[0]);
                }
                if (Most > Margin) {
                    for (double[] Each : Uvs) {
                        if (Each[0] > 1.0) {
                            Each[0] -= 1.0;
                        }
                    }
                }
                Vec3[] Points = new Vec3[4];
                for (int Index = 0; Index < 4; Index++) {
                    double Lx = Corners[Index][0];
                    double Ly = Corners[Index][1];
                    double Wx = Cx + RightAxis.x * Lx - Pos.getX();
                    double Wz = Cz + RightAxis.z * Lx - Pos.getZ();
                    Points[Index] = new Vec3(Wx, Centre.getY() + Ly - Pos.getY(), Wz);
                }
                Vec3 Normal = FrontAxis;
                for (int Side = 0; Side < 2; Side++) {
                    int[] Order = Side == 0 ? new int[]{0, 1, 2, 3} : new int[]{3, 2, 1, 0};
                    Vec3 Facing = Side == 0 ? Normal : Normal.scale(-1.0);
                    for (int Index : Order) {
                        Vec3 At = Points[Index];
                        Consumer.addVertex(Last, (float) At.x, (float) At.y, (float) At.z).setColor(1.0f, 1.0f, 1.0f, 1.0f)
                                .setUv((float) (Math.max(0.0, Uvs[Index][0]) / Margin), (float) Uvs[Index][1]).setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(LightTexture.FULL_BRIGHT).setNormal(Last, (float) Facing.x, (float) Facing.y, (float) Facing.z);
                    }
                }
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(Scp001GatePortalBlockEntity Portal) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 320;
    }

    @Override
    public AABB getRenderBoundingBox(Scp001GatePortalBlockEntity Portal) {
        return new AABB(Portal.getBlockPos());
    }
}
