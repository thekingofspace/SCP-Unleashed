package net.rai.scp_unleashed.scp860.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.scp860.Scp860DoorBlockEntity;
import net.rai.scp_unleashed.scp860.Scp860Registry;
import org.joml.Matrix4f;

public class Scp860DoorRenderer implements BlockEntityRenderer<Scp860DoorBlockEntity> {
    public static final ModelResourceLocation LeafModel = ModelResourceLocation.standalone(Scp860Registry.location("block/scp860/door_leaf"));
    public static final float Swing = -90.0f;
    public static final float HingeX = 13.75f / 16.0f;
    public static final float HingeZ = 4.95f / 16.0f;
    public static final float KeyX = 4.75f / 16.0f;
    public static final float KeyY = 14.75f / 16.0f;
    public static final float KeyZ = 5.0f / 16.0f;
    public static final float RevealLeft = 2.5f / 16.0f;
    public static final float RevealRight = 13.5f / 16.0f;
    public static final float RevealBottom = 1.5f / 16.0f;
    public static final float RevealTop = 29.5f / 16.0f;
    public static final float[] Depths = {5.0f, 2.6f, 1.2f, 0.6f};
    public static final float WindowX = 24.0f;
    public static final float WindowY = 16.0f;
    public static final float LayerSize = 64.0f;
    public static final float ShiftLimit = 22.0f;
    public static final float LiftLimit = 14.0f;
    public static final float MistDrift = 0.05f;
    public static final float LayerGap = 0.004f;
    public static final int Layers = 5;
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final ResourceLocation[] Textures = new ResourceLocation[Layers];
    private final RandomSource Random = RandomSource.create();

    static {
        for (int Index = 0; Index < Layers; Index++) {
            Textures[Index] = Scp860Registry.location("textures/block/scp860/portal_" + Index + ".png");
        }
    }

    public Scp860DoorRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float ease(float Value) {
        return Value * Value * (3.0f - 2.0f * Value);
    }

    public static int turns(Direction Front) {
        return (Front.get2DDataValue() + 2) % 4;
    }

    @Override
    public void render(Scp860DoorBlockEntity Frame, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Frame.getLevel();
        if (World == null) {
            return;
        }
        Direction Front = Frame.facing();
        float Eased = ease(Frame.progress(PartialTick));
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0f * turns(Front)));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        if (Eased > 0.001f && Frame.mode() != Scp860DoorBlockEntity.Arrival) {
            portal(Frame, Front, Pose, Buffers, Eased, World.getGameTime() + PartialTick);
        }
        Pose.pushPose();
        Pose.translate(HingeX, 0.0f, HingeZ);
        Pose.mulPose(Axis.YP.rotationDegrees(Swing * Eased));
        Pose.translate(-HingeX, 0.0f, -HingeZ);
        BakedModel Leaf = Minecraft.getInstance().getModelManager().getModel(LeafModel);
        VertexConsumer Solid = Buffers.getBuffer(RenderType.solid());
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Leaf.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                float Shade = World.getShade(Quad.getDirection(), Quad.isShade());
                Solid.putBulkData(Pose.last(), Quad, Shade, Shade, Shade, 1.0f, PackedLight, PackedOverlay);
            }
        }
        if (Frame.keyed()) {
            Pose.pushPose();
            Pose.translate(KeyX, KeyY, KeyZ);
            Pose.translate(0.0f, 0.0f, -0.05f);
            Pose.mulPose(Axis.YP.rotationDegrees(-90.0f));
            Pose.scale(0.4f, 0.4f, 0.4f);
            Minecraft.getInstance().getItemRenderer().renderStatic(Frame.key(), ItemDisplayContext.NONE, PackedLight, OverlayTexture.NO_OVERLAY, Pose, Buffers, World, 0);
            Pose.popPose();
        }
        Pose.popPose();
        Pose.popPose();
    }

    private void portal(Scp860DoorBlockEntity Frame, Direction Front, PoseStack Pose, MultiBufferSource Buffers, float Eased, float Time) {
        Vec3 Viewer = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 Centre = Vec3.atBottomCenterOf(Frame.getBlockPos()).add(0.0, 1.0, 0.0);
        Vec3 Ray = Centre.subtract(Viewer);
        Direction Across = Front.getClockWise();
        double Rx = Ray.x * Across.getStepX() + Ray.z * Across.getStepZ();
        double Rz = -(Ray.x * Front.getStepX() + Ray.z * Front.getStepZ());
        double Away = Math.max(0.3, Math.abs(Rz));
        float Lateral = (float) (Rx / Away);
        float Vertical = (float) (Ray.y / Away);
        int Alpha = Mth.clamp((int) (Eased * 255.0f), 0, 255);
        Matrix4f Matrix = Pose.last().pose();
        for (int Index = 0; Index < Layers; Index++) {
            float U0;
            float V0;
            float U1;
            float V1;
            if (Index < Depths.length) {
                float Shift = Mth.clamp(-Lateral * Depths[Index] * 3.0f, -ShiftLimit, ShiftLimit);
                float Lift = Mth.clamp(Vertical * Depths[Index] * 2.0f, -LiftLimit, LiftLimit);
                if (Index == 3) {
                    Shift = Mth.clamp(Shift + ((Time * MistDrift) % 12.0f) - 6.0f, -ShiftLimit, ShiftLimit);
                }
                U0 = (WindowX + RevealLeft * 16.0f + Shift) / LayerSize;
                V0 = (WindowY + (2.0f - RevealTop) * 16.0f - Lift) / LayerSize;
                U1 = U0 + (RevealRight - RevealLeft) * 16.0f / LayerSize;
                V1 = V0 + (RevealTop - RevealBottom) * 16.0f / LayerSize;
            } else {
                U0 = 0.0f;
                V0 = 0.0f;
                U1 = 1.0f;
                V1 = 1.0f;
            }
            float Z = 0.5f + LayerGap * (Layers - 1 - Index);
            VertexConsumer Sink = Buffers.getBuffer(RenderType.entityTranslucent(Textures[Index]));
            vertex(Sink, Matrix, Pose, RevealRight, RevealTop, Z, U0, V0, Alpha);
            vertex(Sink, Matrix, Pose, RevealRight, RevealBottom, Z, U0, V1, Alpha);
            vertex(Sink, Matrix, Pose, RevealLeft, RevealBottom, Z, U1, V1, Alpha);
            vertex(Sink, Matrix, Pose, RevealLeft, RevealTop, Z, U1, V0, Alpha);
        }
    }

    private static void vertex(VertexConsumer Sink, Matrix4f Matrix, PoseStack Pose, float X, float Y, float Z, float U, float V, int Alpha) {
        Sink.addVertex(Matrix, X, Y, Z).setColor(255, 255, 255, Alpha).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Pose.last(), 0.0f, 0.0f, -1.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(Scp860DoorBlockEntity Frame) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(Scp860DoorBlockEntity Frame) {
        return Frame.renderBounds();
    }
}
