package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.block.entity.TankBlockEntity;

public class TankRenderer implements BlockEntityRenderer<TankBlockEntity> {
    private static final float Inset = 1.0f / 16.0f;
    private static final float Low = -1.0f + Inset;
    private static final float High = 2.0f - Inset;

    public TankRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(TankBlockEntity Tank, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        FluidStack Liquid = Tank.getShownLiquid();
        float Fill = Math.min(1.0f, Tank.getShownFill(PartialTick));
        if (Liquid.isEmpty() || Fill <= 0.0f) {
            return;
        }
        float Surface = Low + (High - Low) * Fill;
        FluidRendering.renderBox(Pose, Buffers, Liquid, Tank.getLevel(), Tank.getBlockPos(), Low, Low, Low, High, Surface, High, PackedLight, true, true);
    }

    @Override
    public boolean shouldRenderOffScreen(TankBlockEntity Tank) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TankBlockEntity Tank) {
        return Tank.getRenderBounds();
    }
}
