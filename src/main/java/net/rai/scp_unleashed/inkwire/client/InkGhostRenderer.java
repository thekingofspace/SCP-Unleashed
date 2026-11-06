package net.rai.scp_unleashed.inkwire.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.rai.scp_unleashed.inkwire.InkMarks;

public class InkGhostRenderer implements BlockEntityRenderer<BlockEntity> {
    public InkGhostRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(BlockEntity Tile, float PartialTick, PoseStack Poses, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        if (InkGhosts.revealing() && InkMarks.isHidden(Tile.getBlockState())) {
            InkGhosts.block(Tile, Poses, Buffers, PackedLight);
        }
    }
}
