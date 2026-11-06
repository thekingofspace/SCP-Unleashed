package net.rai.scp_unleashed.scp2317.client;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.scp2317.Scp2317DoorBlock;
import net.rai.scp_unleashed.scp2317.Scp2317DoorBlockEntity;
import net.rai.scp_unleashed.scp2317.Scp2317Registry;
import org.joml.Matrix4f;

public class Scp2317DoorRenderer implements BlockEntityRenderer<Scp2317DoorBlockEntity> {
    public static final ModelResourceLocation LeftLeaf = ModelResourceLocation.standalone(Scp2317Registry.location("block/scp2317/leaf_left"));
    public static final ModelResourceLocation RightLeaf = ModelResourceLocation.standalone(Scp2317Registry.location("block/scp2317/leaf_right"));
    public static final float Swing = 92.0f;
    public static final float FarSwing = 96.0f;
    public static final float LeftHinge = 4.6f / 16.0f;
    public static final float RightHinge = 27.4f / 16.0f;
    public static final float HingeZ = 6.2f / 16.0f;
    public static final float Lift = 0.5f;
    public static final float RevealLeft = 4.6f / 16.0f;
    public static final float RevealRight = 27.4f / 16.0f;
    public static final float RevealBottom = 1.0f / 16.0f;
    public static final float RevealTop = 36.0f / 16.0f;
    public static final float[] Depths = {6.0f, 3.0f, 1.4f, 0.6f};
    public static final float LayerGap = 0.004f;
    public static final int Layers = 4;
    public static final float RattleDegrees = 2.2f;
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final ResourceLocation[] Textures = new ResourceLocation[Layers];
    private final RandomSource Random = RandomSource.create();

    static {
        for (int Index = 0; Index < Layers; Index++) {
            Textures[Index] = Scp2317Registry.location("textures/block/scp2317/portal_" + Index + ".png");
        }
    }

    public Scp2317DoorRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float ease(float Value) {
        return Value * Value * (3.0f - 2.0f * Value);
    }

    public static int turns(Direction Front) {
        return (Front.get2DDataValue() + 2) % 4;
    }

    @Override
    public void render(Scp2317DoorBlockEntity Frame, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Frame.getLevel();
        if (World == null) {
            return;
        }
        Direction Front = Frame.facing();
        float Eased = ease(Frame.progress(PartialTick));
        float Time = World.getGameTime() + PartialTick;
        float Shake = Frame.rattle(PartialTick) * RattleDegrees * Mth.sin(Time * 2.6f);
        float Open = (Frame.far() ? FarSwing : Swing) * Eased;
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0f * turns(Front)));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        if (Eased > 0.001f) {
            portal(Frame, Pose, Buffers, Eased, Time);
        }
        leaf(Pose, Buffers, World, LeftLeaf, LeftHinge, Open + Shake, PackedLight, PackedOverlay);
        leaf(Pose, Buffers, World, RightLeaf, RightHinge, -Open - Shake, PackedLight, PackedOverlay);
        Pose.popPose();
    }

    private void leaf(PoseStack Pose, MultiBufferSource Buffers, Level World, ModelResourceLocation Location, float Hinge, float Degrees, int PackedLight, int PackedOverlay) {
        BakedModel Leaf = Minecraft.getInstance().getModelManager().getModel(Location);
        Pose.pushPose();
        Pose.translate(Hinge, Lift, HingeZ);
        Pose.mulPose(Axis.YP.rotationDegrees(Degrees));
        Pose.translate(-Hinge, 0.0f, -HingeZ);
        VertexConsumer Solid = Buffers.getBuffer(RenderType.cutout());
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Leaf.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                float Shade = World.getShade(Quad.getDirection(), Quad.isShade());
                Solid.putBulkData(Pose.last(), Quad, Shade, Shade, Shade, 1.0f, PackedLight, PackedOverlay);
            }
        }
        Pose.popPose();
    }

    private void portal(Scp2317DoorBlockEntity Frame, PoseStack Pose, MultiBufferSource Buffers, float Eased, float Time) {
        Direction Front = Frame.facing();
        Vec3 Viewer = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 Centre = Scp2317DoorBlock.world(Frame.getBlockPos(), Front, 1.0, 1.1, 0.5);
        Vec3 Ray = Centre.subtract(Viewer);
        Direction Across = Front.getClockWise();
        double Rx = Ray.x * Across.getStepX() + Ray.z * Across.getStepZ();
        double Rz = -(Ray.x * Front.getStepX() + Ray.z * Front.getStepZ());
        double Away = Math.max(0.3, Math.abs(Rz));
        float Lateral = (float) (Rx / Away);
        float Vertical = (float) (Ray.y / Away);
        int Alpha = Mth.clamp((int) (Eased * 255.0f), 0, 255);
        int Tint = Frame.far() ? 150 : 255;
        Matrix4f Matrix = Pose.last().pose();
        for (int Index = 0; Index < Layers; Index++) {
            float Shift = Index == 0 ? 0.0f : Mth.clamp(-Lateral * Depths[Index] * 2.0f, -14.0f, 14.0f) + (Index == 3 ? Mth.sin(Time * 0.01f) * 3.0f : 0.0f);
            float Rise = Index == 0 ? 0.0f : Mth.clamp(Vertical * Depths[Index] * 1.5f, -10.0f, 10.0f);
            float U0 = (18.0f + Shift) / 64.0f;
            float U1 = U0 + 23.0f / 64.0f;
            float V0 = (24.0f - Rise) / 64.0f;
            float V1 = V0 + 36.0f / 64.0f;
            float Z = 0.5f + LayerGap * (Layers - 1 - Index);
            VertexConsumer Sink = Buffers.getBuffer(RenderType.entityTranslucent(Textures[Index]));
            vertex(Sink, Matrix, Pose, RevealRight, RevealTop, Z, U0, V0, Alpha, Tint);
            vertex(Sink, Matrix, Pose, RevealRight, RevealBottom, Z, U0, V1, Alpha, Tint);
            vertex(Sink, Matrix, Pose, RevealLeft, RevealBottom, Z, U1, V1, Alpha, Tint);
            vertex(Sink, Matrix, Pose, RevealLeft, RevealTop, Z, U1, V0, Alpha, Tint);
            vertex(Sink, Matrix, Pose, RevealLeft, RevealTop, Z, U1, V0, Alpha, Tint);
            vertex(Sink, Matrix, Pose, RevealLeft, RevealBottom, Z, U1, V1, Alpha, Tint);
            vertex(Sink, Matrix, Pose, RevealRight, RevealBottom, Z, U0, V1, Alpha, Tint);
            vertex(Sink, Matrix, Pose, RevealRight, RevealTop, Z, U0, V0, Alpha, Tint);
        }
    }

    private static void vertex(VertexConsumer Sink, Matrix4f Matrix, PoseStack Pose, float X, float Y, float Z, float U, float V, int Alpha, int Tint) {
        Sink.addVertex(Matrix, X, Y, Z).setColor(Tint, Tint, Tint, Alpha).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Pose.last(), 0.0f, 0.0f, -1.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(Scp2317DoorBlockEntity Frame) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(Scp2317DoorBlockEntity Frame) {
        return Frame.renderBounds();
    }
}
