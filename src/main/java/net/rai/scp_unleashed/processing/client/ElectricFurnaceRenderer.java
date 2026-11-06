package net.rai.scp_unleashed.processing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.processing.ElectricFurnaceBlockEntity;

public class ElectricFurnaceRenderer implements BlockEntityRenderer<ElectricFurnaceBlockEntity> {
    public static final float Pixel = 1.0f / 16.0f;
    public static final float RackTop = 3.5f * Pixel;
    public static final float ChamberDepth = 7.0f * Pixel;
    public static final float PieceScale = 0.28f;
    public static final int GlowLight = 15;

    private final ItemRenderer ItemPainter;

    public ElectricFurnaceRenderer(BlockEntityRendererProvider.Context Context) {
        ItemPainter = Context.getItemRenderer();
    }

    @Override
    public void render(ElectricFurnaceBlockEntity Furnace, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Furnace.getLevel();
        ItemStack Piece = Furnace.getShownWorkpiece();
        if (World == null || Piece.isEmpty()) {
            return;
        }
        int Light = Furnace.isWorking() ? LightTexture.pack(GlowLight, LightTexture.sky(PackedLight)) : PackedLight;
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(ItemPressRenderer.facingTurn(Furnace.getBlockState())));
        Pose.translate(0.0f, RackTop + PieceScale * 0.5f, ChamberDepth - 0.5f);
        Pose.scale(PieceScale, PieceScale, PieceScale);
        ItemPainter.renderStatic(Piece, ItemDisplayContext.FIXED, Light, PackedOverlay, Pose, Buffers, World, (int) Furnace.getBlockPos().asLong());
        Pose.popPose();
    }
}
