package net.rai.scp_unleashed.scp323.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class Scp323HornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final ModelPart Horns;

    public Scp323HornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> Parent, EntityModelSet Models) {
        super(Parent);
        Horns = Models.bakeLayer(Scp323HornShape.LayerLocation);
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int Light, AbstractClientPlayer User, float LimbSwing, float LimbAmount, float Partial, float Age,
                       float HeadYaw, float HeadPitch) {
        if (!Scp323ClientBonds.horned(User) || User.isInvisible()) {
            return;
        }
        Pose.pushPose();
        getParentModel().head.translateAndRotate(Pose);
        Horns.render(Pose, Buffers.getBuffer(RenderType.entityCutoutNoCull(Scp323HornShape.Texture)), Light, OverlayTexture.NO_OVERLAY);
        Pose.popPose();
    }
}
