package net.rai.scp_unleashed.rifttech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rifttech.CollectorTuning;
import net.rai.scp_unleashed.rifttech.RiftCollectorBlock;
import net.rai.scp_unleashed.rifttech.RiftCollectorBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectorRenderer implements BlockEntityRenderer<RiftCollectorBlockEntity> {
    public static final ModelResourceLocation YawModel = model("collector_yaw");
    public static final ModelResourceLocation HeadModel = model("collector_head");
    public static final ModelResourceLocation HeadActiveModel = model("collector_head_active");
    public static final float PivotY = (float) (CollectorTuning.PivotHeight - 1.0);

    private final RandomSource Random = RandomSource.create();
    private final Map<ModelResourceLocation, Cached> Cache = new HashMap<>();

    public CollectorRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static ModelResourceLocation model(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/rifttech/" + Name));
    }

    @Override
    public void render(RiftCollectorBlockEntity Collector, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Collector.getLevel();
        BlockState State = Collector.getBlockState();
        if (World == null || !(State.getBlock() instanceof RiftCollectorBlock)) {
            return;
        }
        int Light = LevelRenderer.getLightColor(World, Collector.getBlockPos().above());
        float Yaw = Collector.getClientYaw(PartialTick);
        float Pitch = Collector.getClientPitch(PartialTick);
        boolean Lit = Collector.isBeaming();
        Pose.pushPose();
        Pose.translate(0.5f, 1.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(Yaw));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        draw(Pose, Buffers, YawModel, Light);
        Pose.translate(0.5f, PivotY, 0.5f);
        Pose.mulPose(Axis.XP.rotationDegrees(Pitch));
        Pose.translate(-0.5f, -PivotY, -0.5f);
        draw(Pose, Buffers, Lit ? HeadActiveModel : HeadModel, Light);
        Pose.popPose();
    }

    private void draw(PoseStack Pose, MultiBufferSource Buffers, ModelResourceLocation Location, int Light) {
        List<BakedQuad> Quads = quads(Location);
        if (Quads.isEmpty()) {
            return;
        }
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose Last = Pose.last();
        for (BakedQuad Quad : Quads) {
            Consumer.putBulkData(Last, Quad, 1.0f, 1.0f, 1.0f, 1.0f, Light, OverlayTexture.NO_OVERLAY);
        }
    }

    private List<BakedQuad> quads(ModelResourceLocation Location) {
        BakedModel Baked = Minecraft.getInstance().getModelManager().getModel(Location);
        Cached Known = Cache.get(Location);
        if (Known == null || Known.Model() != Baked) {
            Known = new Cached(Baked, collect(Baked));
            Cache.put(Location, Known);
        }
        return Known.Quads();
    }

    private record Cached(BakedModel Model, List<BakedQuad> Quads) {
    }

    private List<BakedQuad> collect(BakedModel Baked) {
        List<BakedQuad> Found = new ArrayList<>();
        for (Direction Side : Direction.values()) {
            Random.setSeed(42L);
            Found.addAll(Baked.getQuads(null, Side, Random, ModelData.EMPTY, null));
        }
        Random.setSeed(42L);
        Found.addAll(Baked.getQuads(null, null, Random, ModelData.EMPTY, null));
        return Found;
    }

    @Override
    public AABB getRenderBoundingBox(RiftCollectorBlockEntity Collector) {
        return new AABB(Collector.getBlockPos()).expandTowards(0.0, 1.0, 0.0).inflate(0.5);
    }
}
