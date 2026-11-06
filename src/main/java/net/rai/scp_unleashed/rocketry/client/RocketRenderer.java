package net.rai.scp_unleashed.rocketry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.rocketry.rocket.RocketEntity;

public class RocketRenderer extends EntityRenderer<RocketEntity> {
    public static final float WobbleDegrees = 0.6f;

    public RocketRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.shadowRadius = 1.0f;
    }

    @Override
    public boolean shouldRender(RocketEntity Rocket, Frustum View, double CameraX, double CameraY, double CameraZ) {
        if (!Rocket.shouldRender(CameraX, CameraY, CameraZ)) {
            return false;
        }
        AABB Box = Rocket.getBoundingBox().inflate(1.0);
        return View.isVisible(Box);
    }

    @Override
    public void render(RocketEntity Rocket, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Rocket.getYRot()));
        if (Rocket.isLaunching()) {
            float Time = Rocket.tickCount + PartialTick;
            float Strength = Rocket.getPhase() == RocketEntity.PhaseCountdown
                    ? Mth.clamp(Rocket.getPhaseTime(PartialTick) / RocketEntity.CountdownTicks, 0.0f, 1.0f) * 0.5f
                    : Math.max(0.15f, 1.0f - Rocket.getPhaseTime(PartialTick) / 80.0f);
            Pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(Time * 1.7f) * WobbleDegrees * Strength));
            Pose.mulPose(Axis.ZP.rotationDegrees(Mth.cos(Time * 1.3f) * WobbleDegrees * Strength));
        }
        for (int Group = 0; Group < RocketryModels.RocketParts.size(); Group++) {
            RocketryModels.drawGroup(Pose, Buffers, RocketryModels.RocketParts.get(Group), Group, PackedLight, OverlayTexture.NO_OVERLAY);
        }
        Pose.popPose();
        super.render(Rocket, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RocketEntity Rocket) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
