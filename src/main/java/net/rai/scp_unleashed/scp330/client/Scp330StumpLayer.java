package net.rai.scp_unleashed.scp330.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class Scp330StumpLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public Scp330StumpLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> Parent) {
        super(Parent);
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int Light, AbstractClientPlayer User, float LimbSwing, float LimbSwingAmount,
                       float PartialTick, float AgeInTicks, float NetHeadYaw, float HeadPitch) {
        if (User.isInvisible() || !Scp330ClientState.isGreedy(User)) {
            return;
        }
        Scp330Stumps.caps(Pose, Buffers, Light, LivingEntityRenderer.getOverlayCoords(User, 0.0f), getParentModel(), User);
    }
}
