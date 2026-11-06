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
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.steam.SteamTurbineBlock;
import net.rai.scp_unleashed.steam.SteamTurbineBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class SteamTurbineRenderer implements BlockEntityRenderer<SteamTurbineBlockEntity> {
    public static final ModelResourceLocation RotorModel = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/steam/turbine_rotor"));
    public static final float PivotX = 8.0f / 16.0f;
    public static final float PivotY = 17.0f / 16.0f;
    public static final float GhostDepth = 0.02f / 16.0f;
    public static final float BlurStart = 0.4f;
    public static final float BlurStrength = 0.45f;
    public static final float BlurArc = 0.7f;
    public static final int Ghosts = 3;

    private final RandomSource Random = RandomSource.create();
    private BakedModel CachedModel;
    private List<BakedQuad> CachedQuads = List.of();

    public SteamTurbineRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float yaw(Direction Front) {
        return switch (Front) {
            case EAST -> -90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 90.0f;
            default -> 0.0f;
        };
    }

    @Override
    public void render(SteamTurbineBlockEntity Machine, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        BlockState State = Machine.getBlockState();
        if (!(State.getBlock() instanceof SteamTurbineBlock)) {
            return;
        }
        List<BakedQuad> Quads = quads(Minecraft.getInstance().getModelManager().getModel(RotorModel));
        if (Quads.isEmpty()) {
            return;
        }
        float Angle = Machine.getRenderAngle(PartialTick);
        float Spin = Machine.getRenderSpin(PartialTick);
        Pose.pushPose();
        Pose.translate(0.5f, 0.5f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(yaw(State.getValue(SteamTurbineBlock.Facing))));
        Pose.translate(-0.5f, -0.5f, -0.5f);
        draw(Pose, Buffers.getBuffer(Sheets.cutoutBlockSheet()), Quads, Angle, 0.0f, 1.0f, PackedLight);
        float Blur = Mth.clamp((Spin - BlurStart) / (1.0f - BlurStart), 0.0f, 1.0f);
        if (Blur > 0.0f) {
            VertexConsumer Ghost = Buffers.getBuffer(Sheets.translucentCullBlockSheet());
            float Sweep = Spin * SteamTurbineBlockEntity.MaxSpinDegrees * BlurArc;
            for (int Index = 1; Index <= Ghosts; Index++) {
                float Alpha = BlurStrength * Blur * Blur * (1.0f - (Index - 1.0f) / Ghosts);
                draw(Pose, Ghost, Quads, Angle - Sweep * Index / Ghosts, GhostDepth * Index, Alpha, PackedLight);
            }
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

    private List<BakedQuad> quads(BakedModel Baked) {
        if (Baked != CachedModel) {
            CachedModel = Baked;
            CachedQuads = collect(Baked);
        }
        return CachedQuads;
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
    public boolean shouldRenderOffScreen(SteamTurbineBlockEntity Machine) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(SteamTurbineBlockEntity Machine) {
        return Machine.getRenderBounds();
    }
}
