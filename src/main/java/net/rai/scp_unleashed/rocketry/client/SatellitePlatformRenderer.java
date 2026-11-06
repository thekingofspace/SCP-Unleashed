package net.rai.scp_unleashed.rocketry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rocketry.platform.SatellitePlatformBlock;
import net.rai.scp_unleashed.rocketry.platform.SatellitePlatformBlockEntity;

public class SatellitePlatformRenderer implements BlockEntityRenderer<SatellitePlatformBlockEntity> {
    public SatellitePlatformRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(SatellitePlatformBlockEntity Platform, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        if (Platform.getShownPayload().isEmpty() || !(Platform.getBlockState().getBlock() instanceof SatellitePlatformBlock)) {
            return;
        }
        Vec3 At = Platform.standAt();
        Pose.pushPose();
        Pose.translate(At.x - Platform.getBlockPos().getX(), At.y - Platform.getBlockPos().getY(), At.z - Platform.getBlockPos().getZ());
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Platform.getFront().toYRot()));
        RocketryModels.drawGroup(Pose, Buffers, RocketryModels.Booster, 0, PackedLight, PackedOverlay);
        Pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(SatellitePlatformBlockEntity Platform) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(SatellitePlatformBlockEntity Platform) {
        return SatellitePlatformBlock.Layout.bounds(Platform.getBlockPos(), Platform.getFront()).inflate(0.5);
    }
}
