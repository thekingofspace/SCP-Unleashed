package net.rai.scp_unleashed.scp244.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.scp244.Scp244Style;
import net.rai.scp_unleashed.scp244.Scp244VaseBlock;
import net.rai.scp_unleashed.scp244.Scp244VaseBlockEntity;

import java.util.EnumMap;
import java.util.Map;

public class Scp244VaseRenderer implements BlockEntityRenderer<Scp244VaseBlockEntity> {
    public static final int ViewDistance = 112;
    private static final Map<Scp244Style, ModelResourceLocation> Lids = new EnumMap<>(Scp244Style.class);
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final RandomSource Random = RandomSource.create();

    static {
        for (Scp244Style Style : Scp244Style.values()) {
            Lids.put(Style, ModelResourceLocation.standalone(Style.lidModel()));
        }
    }

    public Scp244VaseRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static Iterable<ModelResourceLocation> lidModels() {
        return Lids.values();
    }

    public static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static void lidPose(PoseStack Pose, Scp244Style Style, Direction Front, float Progress) {
        float Rise = smooth(Progress / Style.liftShare());
        float Swing = smooth((Progress - Style.liftShare()) / (1.0f - Style.liftShare()));
        float Up = Style.liftPixels() * Rise * (1.0f - Swing);
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Front.toYRot() + Style.lidTurn()));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Pose.translate(0.0f, Up / 16.0f, 0.0f);
        Pose.translate(Style.pivotX() / 16.0f, Style.pivotY() / 16.0f, 0.0f);
        Pose.mulPose(Axis.ZP.rotationDegrees(Style.tiltDegrees() * Swing));
        Pose.translate(-Style.pivotX() / 16.0f, -Style.pivotY() / 16.0f, 0.0f);
        Pose.translate(-Style.slidePixels() * Swing / 16.0f, 0.0f, 0.0f);
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-Style.lidTurn()));
        Pose.translate(-0.5f, 0.0f, -0.5f);
    }

    @Override
    public void render(Scp244VaseBlockEntity Vase, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Vase.getLevel();
        BlockState State = Vase.getBlockState();
        if (World == null || !(State.getBlock() instanceof Scp244VaseBlock Vessel)) {
            return;
        }
        Scp244Style Style = Vessel.style();
        Direction Front = State.getValue(Scp244VaseBlock.Facing);
        BlockPos Pos = Vase.getBlockPos();
        int Light = Math.max(LevelRenderer.getLightColor(World, Pos), LevelRenderer.getLightColor(World, Pos.above()));
        BakedModel Model = Minecraft.getInstance().getModelManager().getModel(Lids.get(Style));
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.solid());
        Pose.pushPose();
        lidPose(Pose, Style, Front, Vase.lid(PartialTick));
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                float Shade = World.getShade(Quad.getDirection(), Quad.isShade());
                Consumer.putBulkData(Pose.last(), Quad, Shade, Shade, Shade, 1.0f, Light, PackedOverlay);
            }
        }
        Pose.popPose();
        Scp244FogRenderer.render(Vase, PartialTick, Pose, Buffers);
    }

    @Override
    public boolean shouldRenderOffScreen(Scp244VaseBlockEntity Vase) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return ViewDistance;
    }

    @Override
    public AABB getRenderBoundingBox(Scp244VaseBlockEntity Vase) {
        Scp244FogEffect Fog = Scp244FogEffect.peek(Vase);
        AABB Bounds = Fog == null || !Fog.showing() ? null : Fog.bounds();
        BlockPos Pos = Vase.getBlockPos();
        AABB Own = new AABB(Pos.getX() - 1.0, Pos.getY(), Pos.getZ() - 1.0, Pos.getX() + 2.0, Pos.getY() + 1.5, Pos.getZ() + 2.0);
        return Bounds != null ? Bounds.minmax(Own) : Own;
    }
}
