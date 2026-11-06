package net.rai.scp_unleashed.rocketry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RocketryModels {
    public static final ModelResourceLocation RocketLower = model("rocket_lower");
    public static final ModelResourceLocation RocketMiddle = model("rocket_middle");
    public static final ModelResourceLocation RocketUpper = model("rocket_upper");
    public static final ModelResourceLocation DropPod = model("drop_pod");
    public static final ModelResourceLocation DropPodLit = model("drop_pod_lit");
    public static final ModelResourceLocation Booster = model("satellite_booster");
    public static final ModelResourceLocation BoosterLit = model("satellite_booster_lit");
    public static final List<ModelResourceLocation> RocketParts = List.of(RocketLower, RocketMiddle, RocketUpper);
    public static final List<ModelResourceLocation> All = List.of(RocketLower, RocketMiddle, RocketUpper, DropPod, DropPodLit, Booster, BoosterLit);
    public static final float GroupPixels = 48.0f;
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final RandomSource Random = RandomSource.create();
    private static final Map<ModelResourceLocation, List<BakedQuad>> Quads = new ConcurrentHashMap<>();

    private RocketryModels() {
    }

    private static ModelResourceLocation model(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/rocketry/entity/" + Name));
    }

    public static void drawGroup(PoseStack Pose, MultiBufferSource Buffers, ModelResourceLocation Location, int Group, int PackedLight, int PackedOverlay) {
        Pose.pushPose();
        Pose.translate(-0.5f, (Group * GroupPixels + 16.0f) / 16.0f, -0.5f);
        draw(Pose, Buffers, Location, PackedLight, PackedOverlay);
        Pose.popPose();
    }

    public static void clear() {
        Quads.clear();
    }

    private static List<BakedQuad> quads(ModelResourceLocation Location) {
        return Quads.computeIfAbsent(Location, Wanted -> {
            BakedModel Model = Minecraft.getInstance().getModelManager().getModel(Wanted);
            List<BakedQuad> Gathered = new ArrayList<>();
            for (Direction Side : QuadSides) {
                Random.setSeed(42L);
                Gathered.addAll(Model.getQuads(null, Side, Random, ModelData.EMPTY, null));
            }
            return List.copyOf(Gathered);
        });
    }

    public static void draw(PoseStack Pose, MultiBufferSource Buffers, ModelResourceLocation Location, int PackedLight, int PackedOverlay) {
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.solidBlockSheet());
        PoseStack.Pose Last = Pose.last();
        for (BakedQuad Quad : quads(Location)) {
            Consumer.putBulkData(Last, Quad, 1.0f, 1.0f, 1.0f, 1.0f, PackedLight, PackedOverlay);
        }
    }
}
