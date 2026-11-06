package net.rai.scp_unleashed.steam.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlock;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndustrialTurbineRenderer implements BlockEntityRenderer<IndustrialTurbineBlockEntity> {
    public static final ModelResourceLocation FrontModel = SteamTurbineRenderer.RotorModel;
    public static final List<ModelResourceLocation> StageModels = List.of(stage("industrial_rotor_hp"), stage("industrial_rotor_ip"), stage("industrial_rotor_lp"));
    public static final List<ModelResourceLocation> GlassModels = List.of(stage("industrial_glass_0"), stage("industrial_glass_1"), stage("industrial_glass_2"));
    public static final List<ModelResourceLocation> SteamModels = List.of(stage("industrial_steam_0"), stage("industrial_steam_1"), stage("industrial_steam_2"));
    public static final float[] StageDepths = {8.0f / 16.0f, 39.0f / 16.0f, 71.0f / 16.0f};
    public static final float[] GlassDepths = {15.0f / 16.0f, 43.0f / 16.0f, 74.0f / 16.0f};
    public static final float[] SteamDepths = {9.0f / 16.0f, 40.0f / 16.0f, 71.0f / 16.0f};
    public static final float MaxSteam = 0.9f;
    public static final float SteamFloor = 0.004f;
    public static final int SteamLayers = 2;
    public static final float PivotX = 8.0f / 16.0f;
    public static final float PivotY = 17.0f / 16.0f;
    public static final float GhostDepth = 0.02f / 16.0f;
    public static final float BlurStart = 0.4f;
    public static final float BlurStrength = 0.4f;
    public static final float BlurArc = 0.7f;
    public static final int Ghosts = 3;

    private final RandomSource Random = RandomSource.create();
    private final Map<ModelResourceLocation, BakedModel> CachedModels = new HashMap<>();
    private final Map<ModelResourceLocation, List<BakedQuad>> CachedQuads = new HashMap<>();

    public IndustrialTurbineRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static List<ModelResourceLocation> models() {
        List<ModelResourceLocation> All = new ArrayList<>(StageModels);
        All.addAll(GlassModels);
        All.addAll(SteamModels);
        return All;
    }

    public static float layerAlpha(float Fill) {
        float Total = MaxSteam * Mth.clamp(Fill, 0.0f, 1.0f);
        return 1.0f - (float) Math.pow(1.0f - Total, 1.0f / SteamLayers);
    }

    private static ModelResourceLocation stage(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/steam/" + Name));
    }

    @Override
    public void render(IndustrialTurbineBlockEntity Machine, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        BlockState State = Machine.getBlockState();
        if (!(State.getBlock() instanceof IndustrialTurbineBlock)) {
            return;
        }
        ModelManager Models = Minecraft.getInstance().getModelManager();
        List<List<BakedQuad>> Parts = new ArrayList<>();
        List<Float> Depths = new ArrayList<>();
        List<BakedQuad> Front = quads(FrontModel, Models.getModel(FrontModel));
        if (!Front.isEmpty()) {
            Parts.add(Front);
            Depths.add(0.0f);
        }
        for (int Index = 0; Index < StageModels.size(); Index++) {
            List<BakedQuad> Stage = quads(StageModels.get(Index), Models.getModel(StageModels.get(Index)));
            if (!Stage.isEmpty()) {
                Parts.add(Stage);
                Depths.add(StageDepths[Index]);
            }
        }
        if (Parts.isEmpty()) {
            return;
        }
        float Angle = Machine.getRenderAngle(PartialTick);
        float Spin = Machine.getRenderSpin(PartialTick);
        Pose.pushPose();
        Pose.translate(0.5f, 0.5f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(SteamTurbineRenderer.yaw(State.getValue(IndustrialTurbineBlock.Facing))));
        Pose.translate(-0.5f, -0.5f, -0.5f);
        VertexConsumer Solid = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        for (int Index = 0; Index < Parts.size(); Index++) {
            draw(Pose, Solid, Parts.get(Index), Angle, Depths.get(Index), 1.0f, PackedLight);
        }
        float Blur = Mth.clamp((Spin - BlurStart) / (1.0f - BlurStart), 0.0f, 1.0f);
        if (Blur > 0.0f) {
            VertexConsumer Ghost = Buffers.getBuffer(Sheets.translucentCullBlockSheet());
            float Sweep = Spin * IndustrialTurbineBlockEntity.MaxSpinDegrees * BlurArc;
            for (int Step = 1; Step <= Ghosts; Step++) {
                float Alpha = BlurStrength * Blur * Blur * (1.0f - (Step - 1.0f) / Ghosts);
                for (int Index = 0; Index < Parts.size(); Index++) {
                    draw(Pose, Ghost, Parts.get(Index), Angle - Sweep * Step / Ghosts, Depths.get(Index) + GhostDepth * Step, Alpha, PackedLight);
                }
            }
        }
        VertexConsumer Clear = Buffers.getBuffer(Sheets.translucentCullBlockSheet());
        for (int Index = 0; Index < SteamModels.size(); Index++) {
            float Fill = Machine.getRenderFill(Index, PartialTick);
            if (Fill > SteamFloor) {
                List<BakedQuad> Steam = quads(SteamModels.get(Index), Models.getModel(SteamModels.get(Index)));
                draw(Pose, Clear, Steam, 0.0f, SteamDepths[Index], layerAlpha(Fill), PackedLight);
            }
        }
        for (int Index = 0; Index < GlassModels.size(); Index++) {
            List<BakedQuad> Glass = quads(GlassModels.get(Index), Models.getModel(GlassModels.get(Index)));
            draw(Pose, Clear, Glass, 0.0f, GlassDepths[Index], 1.0f, PackedLight);
        }
        Pose.popPose();
    }

    private void draw(PoseStack Pose, VertexConsumer Consumer, List<BakedQuad> Quads, float Angle, float Depth, float Alpha, int Light) {
        Pose.pushPose();
        Pose.translate(PivotX, PivotY, Depth);
        Pose.mulPose(Axis.ZP.rotationDegrees(Angle));
        Pose.translate(-PivotX, -PivotY, 0.0f);
        PoseStack.Pose Last = Pose.last();
        for (BakedQuad Quad : Quads) {
            Consumer.putBulkData(Last, Quad, 1.0f, 1.0f, 1.0f, Alpha, Light, OverlayTexture.NO_OVERLAY);
        }
        Pose.popPose();
    }

    private List<BakedQuad> quads(ModelResourceLocation Location, BakedModel Baked) {
        if (CachedModels.get(Location) != Baked) {
            CachedModels.put(Location, Baked);
            CachedQuads.put(Location, collect(Baked));
        }
        return CachedQuads.getOrDefault(Location, List.of());
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
    public boolean shouldRenderOffScreen(IndustrialTurbineBlockEntity Machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(IndustrialTurbineBlockEntity Machine) {
        return Machine.getRenderBounds();
    }
}
