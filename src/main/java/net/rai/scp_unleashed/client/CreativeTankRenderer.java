package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.block.entity.CreativeTankBlockEntity;

public class CreativeTankRenderer implements BlockEntityRenderer<CreativeTankBlockEntity> {
    private static final float Wall = 1.5f / 16.0f;
    private static final float Floor = 2.0f / 16.0f;
    private static final float Roof = 14.0f / 16.0f;

    public CreativeTankRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(CreativeTankBlockEntity Tank, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        FluidStack Liquid = Tank.getLiquid();
        if (Liquid.isEmpty()) {
            return;
        }
        FluidRendering.renderBox(Pose, Buffers, Liquid, Tank.getLevel(), Tank.getBlockPos(), Wall, Floor, Wall, 1.0f - Wall, Roof, 1.0f - Wall, PackedLight, true, true);
    }
}
