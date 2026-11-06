package net.rai.scp_unleashed.rocketry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.rai.scp_unleashed.rocketry.rocket.DropPodEntity;

public class DropPodRenderer extends EntityRenderer<DropPodEntity> {
    public static final float SwayDegrees = 1.4f;

    public DropPodRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.shadowRadius = 0.9f;
    }

    @Override
    public void render(DropPodEntity Pod, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Pod.getYRot()));
        if (!Pod.isLanded()) {
            float Time = Pod.tickCount + PartialTick;
            float Strength = Pod.isThrusting() ? 1.0f : 0.5f;
            Pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(Time * 0.9f) * SwayDegrees * Strength));
            Pose.mulPose(Axis.ZP.rotationDegrees(Mth.cos(Time * 0.7f) * SwayDegrees * Strength));
        }
        RocketryModels.drawGroup(Pose, Buffers, Pod.isThrusting() ? RocketryModels.DropPodLit : RocketryModels.DropPod, 0, PackedLight, OverlayTexture.NO_OVERLAY);
        Pose.popPose();
        super.render(Pod, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DropPodEntity Pod) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
