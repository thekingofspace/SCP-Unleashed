package net.rai.scp_unleashed.scp409.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scp409Crystals {
    public static final int CoreGlow = 15;
    public static final int ShellGlow = 12;
    public static final double CoreDistance = 28.0;
    public static final ModelResourceLocation CaseBase = model("block/scp409_case");
    public static final ModelResourceLocation CaseLid = model("block/scp409/case_lid");
    public static final ModelResourceLocation CaseShell = model("block/scp409/case_crystal_shell");
    public static final ModelResourceLocation CaseCore = model("block/scp409/case_crystal_core");
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final RandomSource Seeded = RandomSource.create();
    private static final Map<ModelResourceLocation, List<BakedQuad>> Quads = new HashMap<>();

    private Scp409Crystals() {
    }

    public static ModelResourceLocation model(String Path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    public static ModelResourceLocation shell(int Variant) {
        return model("block/scp409/cluster_" + Variant + "_shell");
    }

    public static ModelResourceLocation core(int Variant) {
        return model("block/scp409/cluster_" + Variant + "_core");
    }

    public static List<ModelResourceLocation> models() {
        List<ModelResourceLocation> Found = new ArrayList<>(List.of(CaseBase, CaseLid, CaseShell, CaseCore));
        for (int Variant = 0; Variant < Scp409Anchors.Variants; Variant++) {
            Found.add(shell(Variant));
            Found.add(core(Variant));
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

    public static int glow(int Light, int Level) {
        return LightTexture.pack(Math.max(LightTexture.block(Light), Level), LightTexture.sky(Light));
    }

    public static void solid(PoseStack Pose, MultiBufferSource Buffers, ModelResourceLocation Location, int Light) {
        put(Pose.last(), Buffers.getBuffer(Sheets.cutoutBlockSheet()), quads(Location), Light);
    }

    public static void crystal(PoseStack Pose, MultiBufferSource Buffers, ModelResourceLocation Shell, ModelResourceLocation Core, int Light, boolean WithCore) {
        if (WithCore) {
            put(Pose.last(), Buffers.getBuffer(Sheets.cutoutBlockSheet()), quads(Core), glow(Light, CoreGlow));
        }
        put(Pose.last(), Buffers.getBuffer(Sheets.translucentCullBlockSheet()), quads(Shell), glow(Light, ShellGlow));
    }

    public static void cluster(PoseStack Pose, MultiBufferSource Buffers, int Light, int Variant, float Size, boolean WithCore) {
        Pose.pushPose();
        Pose.scale(Size, Size, Size);
        Pose.translate(-0.5f, -Scp409Anchors.Embed, -0.5f);
        crystal(Pose, Buffers, shell(Variant), core(Variant), Light, WithCore);
        Pose.popPose();
    }

    private static void put(PoseStack.Pose Last, VertexConsumer Consumer, List<BakedQuad> Batch, int Light) {
        for (BakedQuad Quad : Batch) {
            Consumer.putBulkData(Last, Quad, 1.0f, 1.0f, 1.0f, 1.0f, Light, OverlayTexture.NO_OVERLAY);
        }
    }
}
