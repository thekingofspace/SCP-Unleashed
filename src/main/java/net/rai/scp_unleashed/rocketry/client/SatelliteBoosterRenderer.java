package net.rai.scp_unleashed.rocketry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteBoosterEntity;

public class SatelliteBoosterRenderer extends EntityRenderer<SatelliteBoosterEntity> {
    public SatelliteBoosterRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(SatelliteBoosterEntity Booster, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Booster.getYRot()));
        RocketryModels.drawGroup(Pose, Buffers, Booster.isClimbing() ? RocketryModels.BoosterLit : RocketryModels.Booster, 0, PackedLight, OverlayTexture.NO_OVERLAY);
        Pose.popPose();
        super.render(Booster, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SatelliteBoosterEntity Booster) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
