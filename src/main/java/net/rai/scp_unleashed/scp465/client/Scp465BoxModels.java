package net.rai.scp_unleashed.scp465.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.scp465.Scp465Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scp465BoxModels {
    public static final ModelResourceLocation Lid = model("block/scp465/lid");
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final RandomSource Seeded = RandomSource.create();
    private static final Map<ModelResourceLocation, List<BakedQuad>> Quads = new HashMap<>();

    private Scp465BoxModels() {
    }

    public static ModelResourceLocation model(String Path) {
        return ModelResourceLocation.standalone(Scp465Registry.location(Path));
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Lid);
    }

    public static void reload() {
        synchronized (Quads) {
            Quads.clear();
        }
    }

    public static List<BakedQuad> quads(ModelResourceLocation Location) {
        synchronized (Quads) {
            List<BakedQuad> Found = Quads.get(Location);
            if (Found == null) {
                BakedModel Model = Minecraft.getInstance().getModelManager().getModel(Location);
                List<BakedQuad> Collected = new ArrayList<>();
                for (Direction Side : QuadSides) {
                    Seeded.setSeed(42L);
                    Collected.addAll(Model.getQuads(null, Side, Seeded, ModelData.EMPTY, null));
                }
                Found = List.copyOf(Collected);
                Quads.put(Location, Found);
            }
            return Found;
        }
    }

    public static void lid(PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose Last = Pose.last();
        for (BakedQuad Quad : quads(Lid)) {
            Consumer.putBulkData(Last, Quad, 1.0f, 1.0f, 1.0f, 1.0f, Light, Overlay);
        }
    }
}
