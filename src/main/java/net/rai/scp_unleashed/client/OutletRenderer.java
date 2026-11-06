package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.inkwire.InkMarks;
import net.rai.scp_unleashed.inkwire.client.InkGhosts;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.Wires;

public class OutletRenderer implements BlockEntityRenderer<OutletBlockEntity> {
    public OutletRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(OutletBlockEntity Outlet, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Outlet.getLevel();
        if (World == null) {
            return;
        }
        boolean Revealing = InkGhosts.revealing();
        if (Revealing && InkMarks.isHidden(Outlet.getBlockState())) {
            InkGhosts.block(Outlet, Pose, Buffers, PackedLight);
        }
        if (Outlet.getLinks().isEmpty()) {
            return;
        }
        BlockPos Pos = Outlet.getBlockPos();
        Vec3 Origin = Vec3.atLowerCornerOf(Pos);
        for (WireLink Link : Outlet.getLinks()) {
            if (!drawsWire(World, Pos, Link.Target())) {
                continue;
            }
            boolean Hidden = InkMarks.wireHidden(World, Pos, Link);
            if (Hidden && !Revealing) {
                continue;
            }
            Vec3 From = Outlet.attachFor(Link).subtract(Origin);
            Vec3 To = Link.targetAttach().subtract(Origin);
            int LightFrom = WireRenderer.anchorLight(World, Pos, Outlet.faceFor(Link), Outlet.isFullBlock());
            int LightTo = WireRenderer.anchorLight(World, Link.Target(), Link.TargetFacing(), Link.TargetBlock());
            double HalfWidth = Wires.halfWidth(Link.Tier(), Link.Slot(), Pos, Link.Target());
            if (Hidden) {
                WireRenderer.render(Pose, InkGhosts.wireBuffers(Buffers, Link.Tier()), From, To, InkGhosts.glow(LightFrom), InkGhosts.glow(LightTo),
                        InkGhosts.WireColour, Link.Tier(), HalfWidth);
            } else {
                WireRenderer.render(Pose, Buffers, From, To, LightFrom, LightTo, 0xFFFFFFFF, Link.Tier(), HalfWidth);
            }
        }
    }

    private static boolean drawsWire(Level World, BlockPos Pos, BlockPos Target) {
        if (Pos.asLong() < Target.asLong() || !World.isLoaded(Target)) {
            return true;
        }
        return !(World.getBlockEntity(Target) instanceof OutletBlockEntity Other) || !Other.isLinkedTo(Pos);
    }

    @Override
    public boolean shouldRenderOffScreen(OutletBlockEntity Outlet) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(OutletBlockEntity Outlet) {
        AABB Box = new AABB(Outlet.getBlockPos());
        for (WireLink Link : Outlet.getLinks()) {
            Box = Box.minmax(new AABB(Link.Target()));
        }
        return Box;
    }
}
