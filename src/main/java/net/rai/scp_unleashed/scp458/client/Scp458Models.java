package net.rai.scp_unleashed.scp458.client;

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
import net.rai.scp_unleashed.scp458.Scp458Contents;
import net.rai.scp_unleashed.scp458.Scp458Lid;
import net.rai.scp_unleashed.scp458.Scp458Pizza;
import net.rai.scp_unleashed.scp458.Scp458Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Scp458Models {
    public static final ModelResourceLocation Base = model("block/scp458_box");
    public static final ModelResourceLocation Lid = model("block/scp458/lid");
    public static final ModelResourceLocation Hinge = model("block/scp458/hinge");
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final RandomSource Seeded = RandomSource.create();
    private static final Map<ModelResourceLocation, List<BakedQuad>> Quads = new HashMap<>();
    private static final ModelResourceLocation[][] Slices = slices();

    private Scp458Models() {
    }

    public static ModelResourceLocation model(String Path) {
        return ModelResourceLocation.standalone(Scp458Registry.location(Path));
    }

    private static ModelResourceLocation[][] slices() {
        Scp458Pizza[] All = Scp458Pizza.values();
        ModelResourceLocation[][] Made = new ModelResourceLocation[All.length][Scp458Contents.Slices];
        for (Scp458Pizza Each : All) {
            for (int Slice = 0; Slice < Scp458Contents.Slices; Slice++) {
                Made[Each.ordinal()][Slice] = model("block/scp458/pizza/" + Each.id() + "_" + Slice);
            }
        }
        return Made;
    }

    public static ModelResourceLocation slice(Scp458Pizza Shown, int Slice) {
        return Slices[Shown.ordinal()][Slice];
    }

    public static List<ModelResourceLocation> models() {
        List<ModelResourceLocation> Found = new ArrayList<>(List.of(Base, Lid, Hinge));
        for (ModelResourceLocation[] Row : Slices) {
            Found.addAll(List.of(Row));
        }
        return Found;
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

    public static void hinged(PoseStack Pose, Consumer<ModelResourceLocation> Draw, ModelResourceLocation Location, float Angle) {
        Pose.pushPose();
        Pose.translate(0.0f, Scp458Lid.HingeY, Scp458Lid.HingeZ);
        Pose.mulPose(Axis.XP.rotationDegrees(Angle));
        Pose.translate(0.0f, -Scp458Lid.HingeY, -Scp458Lid.HingeZ);
        Draw.accept(Location);
        Pose.popPose();
    }

    public static void draw(PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay, float Angle, @Nullable Scp458Pizza Shown, int Taken) {
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        draw(Pose, Location -> solid(Pose, Consumer, Location, Light, Overlay), Angle, Shown, Taken);
    }

    public static void draw(PoseStack Pose, Consumer<ModelResourceLocation> Draw, float Angle, @Nullable Scp458Pizza Shown, int Taken) {
        Draw.accept(Base);
        if (Shown != null && Angle > Scp458Lid.PizzaShown) {
            for (int Slice = 0; Slice < Scp458Contents.Slices; Slice++) {
                if ((Taken & (1 << Slice)) == 0) {
                    Draw.accept(slice(Shown, Slice));
                }
            }
        }
        hinged(Pose, Draw, Lid, Angle);
        hinged(Pose, Draw, Hinge, Angle * Scp458Lid.HingeShare);
    }
}
