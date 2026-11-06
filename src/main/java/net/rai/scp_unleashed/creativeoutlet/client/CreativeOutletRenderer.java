package net.rai.scp_unleashed.creativeoutlet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.client.OutletRenderer;
import net.rai.scp_unleashed.creativeoutlet.CreativeOutletBlockEntity;

public class CreativeOutletRenderer implements BlockEntityRenderer<CreativeOutletBlockEntity> {
    private final OutletRenderer Wires;

    public CreativeOutletRenderer(BlockEntityRendererProvider.Context Context) {
        Wires = new OutletRenderer(Context);
    }

    @Override
    public void render(CreativeOutletBlockEntity Outlet, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Wires.render(Outlet, PartialTick, Pose, Buffers, PackedLight, PackedOverlay);
    }

    @Override
    public boolean shouldRenderOffScreen(CreativeOutletBlockEntity Outlet) {
        return Wires.shouldRenderOffScreen(Outlet);
    }

    @Override
    public int getViewDistance() {
        return Wires.getViewDistance();
    }

    @Override
    public AABB getRenderBoundingBox(CreativeOutletBlockEntity Outlet) {
        return Wires.getRenderBoundingBox(Outlet);
    }
}
