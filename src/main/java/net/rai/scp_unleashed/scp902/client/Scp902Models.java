package net.rai.scp_unleashed.scp902.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.scp902.Scp902Lid;
import net.rai.scp_unleashed.scp902.Scp902Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Scp902Models {
    public static final ModelResourceLocation Base = model("block/scp902_box");
    public static final ModelResourceLocation Lid = model("block/scp902/lid");
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final RandomSource Seeded = RandomSource.create();
    private static final Map<ModelResourceLocation, List<BakedQuad>> Quads = new HashMap<>();

    private Scp902Models() {
    }

    public static ModelResourceLocation model(String Path) {
        return ModelResourceLocation.standalone(Scp902Registry.location(Path));
    }

    public static List<ModelResourceLocation> models() {
        return List.of(Base, Lid);
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

    public static void solid(PoseStack Pose, VertexConsumer Consumer, ModelResourceLocation Location, int Light, int Overlay) {
        PoseStack.Pose Last = Pose.last();
        for (BakedQuad Quad : quads(Location)) {
            Consumer.putBulkData(Last, Quad, 1.0f, 1.0f, 1.0f, 1.0f, Light, Overlay);
        }
    }

    public static void draw(PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay, float Angle) {
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        draw(Pose, Location -> solid(Pose, Consumer, Location, Light, Overlay), Angle);
    }

    public static void draw(PoseStack Pose, Consumer<ModelResourceLocation> Draw, float Angle) {
        Draw.accept(Base);
        Pose.pushPose();
        Pose.translate(0.0f, Scp902Lid.HingeY, Scp902Lid.HingeZ);
        Pose.mulPose(Axis.XP.rotationDegrees(Angle));
        Pose.translate(0.0f, -Scp902Lid.HingeY, -Scp902Lid.HingeZ);
        Draw.accept(Lid);
        Pose.popPose();
    }
}
