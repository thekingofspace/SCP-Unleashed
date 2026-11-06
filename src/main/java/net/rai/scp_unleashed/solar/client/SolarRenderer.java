package net.rai.scp_unleashed.solar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.solar.SolarBlockEntity;
import net.rai.scp_unleashed.solar.SolarTier;

import java.util.List;

public class SolarRenderer implements BlockEntityRenderer<SolarBlockEntity> {
    public static final ResourceLocation PanelRotor = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/solar/panel_rotor");
    public static final ResourceLocation ArrayRotor = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/solar/array_rotor");
    public static final List<ResourceLocation> Models = List.of(PanelRotor, ArrayRotor);
    public static final float AxleX = 8.0f / 16.0f;
    public static final float AxleY = 8.5f / 16.0f;
    public static final int ArrayRows = 2;

    public SolarRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(SolarBlockEntity Collector, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        boolean Array = Collector.getTier() == SolarTier.Array;
        BakedModel Rotor = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(Array ? ArrayRotor : PanelRotor));
        ModelBlockRenderer Renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        float Tilt = Collector.getRenderTilt(PartialTick);
        int Rows = Array ? ArrayRows : 1;
        for (int Row = 0; Row < Rows; Row++) {
            Pose.pushPose();
            Pose.translate(Row + AxleX, AxleY, 0.0f);
            Pose.mulPose(Axis.ZP.rotationDegrees(Tilt));
            Pose.translate(-AxleX, -AxleY, 0.0f);
            Renderer.renderModel(Pose.last(), Consumer, null, Rotor, 1.0f, 1.0f, 1.0f, PackedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            Pose.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(SolarBlockEntity Collector) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(SolarBlockEntity Collector) {
        return Collector.getRenderBounds();
    }
}
