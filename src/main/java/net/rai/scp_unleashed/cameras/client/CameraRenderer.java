package net.rai.scp_unleashed.cameras.client;

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
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cameras.CameraBlockEntity;
import net.rai.scp_unleashed.cameras.CameraMount;
import org.joml.Vector3f;

public class CameraRenderer implements BlockEntityRenderer<CameraBlockEntity> {
    public static final ModelResourceLocation HeadModel = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/cameras/camera_head"));
    public static final ModelResourceLocation LedModel = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/cameras/camera_led"));
    public static final int IdlePeriod = 40;
    public static final int IdleLit = 6;
    public static final int LivePeriod = 20;
    public static final int LiveLit = 10;
    public static final float DimLed = 0.32f;
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};

    private final RandomSource Random = RandomSource.create();

    public CameraRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static boolean ledLit(long GameTime, BlockPos Pos, boolean Live) {
        int Phase = Math.floorMod(Pos.hashCode(), 40);
        long Time = GameTime + Phase;
        return Live ? Math.floorMod(Time, LivePeriod) < LiveLit : Math.floorMod(Time, IdlePeriod) < IdleLit;
    }

    @Override
    public void render(CameraBlockEntity Camera, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Camera.getLevel();
        if (World == null) {
            return;
        }
        CameraMount Mount = Camera.mount();
        float Pan = Camera.shownPan(PartialTick);
        float Tilt = Camera.shownTilt(PartialTick);
        Vec3 Pivot = Mount.pivotOffset();
        Pose.pushPose();
        Pose.translate(Pivot.x, Pivot.y, Pivot.z);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Mount.yaw(Pan)));
        Pose.mulPose(Axis.XP.rotationDegrees(-Mount.pitch(Tilt)));
        Pose.translate(-CameraMount.PivotX, -CameraMount.PivotY, -CameraMount.PivotZ);
        draw(Pose.last(), Buffers.getBuffer(RenderType.cutout()), model(HeadModel), World, PackedLight, PackedOverlay, 1.0f);
        boolean Lit = ledLit(World.getGameTime(), Camera.getBlockPos(), Camera.getViewers() > 0);
        draw(Pose.last(), Buffers.getBuffer(RenderType.cutout()), model(LedModel), World, Lit ? LightTexture.FULL_BRIGHT : PackedLight, PackedOverlay, Lit ? 1.0f : DimLed);
        Pose.popPose();
    }

    private static BakedModel model(ModelResourceLocation Location) {
        return Minecraft.getInstance().getModelManager().getModel(Location);
    }

    private void draw(PoseStack.Pose Last, VertexConsumer Consumer, BakedModel Model, Level World, int Light, int Overlay, float Tint) {
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                Vector3f Normal = Last.normal().transform(new Vector3f(Quad.getDirection().step()));
                float Shade = Tint >= 1.0f && Light == LightTexture.FULL_BRIGHT ? 1.0f : World.getShade(Direction.getNearest(Normal.x(), Normal.y(), Normal.z()), Quad.isShade());
                Consumer.putBulkData(Last, Quad, Shade * Tint, Shade * Tint, Shade * Tint, 1.0f, Light, Overlay);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(CameraBlockEntity Camera) {
        return Camera.renderBox();
    }
}
