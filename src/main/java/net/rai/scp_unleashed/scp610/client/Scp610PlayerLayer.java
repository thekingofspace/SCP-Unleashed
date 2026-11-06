package net.rai.scp_unleashed.scp610.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp610.Scp610Infection;

public class Scp610PlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public Scp610PlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> Parent) {
        super(Parent);
    }

    public static ResourceLocation texture(int Stage) {
        return Scp610GeoModel.location("textures/entity/scp610/infection_stage_" + Mth.clamp(Stage, 1, Scp610Infection.Stages) + ".png");
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int Light, AbstractClientPlayer Owner, float LimbSwing, float LimbAmount, float Partial, float Age,
                       float HeadYaw, float HeadPitch) {
        int Stage = Scp610ClientState.stage(Owner.getId());
        if (Stage <= 0 || Owner.isInvisible() || Owner.isSpectator()) {
            return;
        }
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.entityCutoutNoCull(texture(Stage)));
        getParentModel().renderToBuffer(Pose, Buffer, Light, LivingEntityRenderer.getOverlayCoords(Owner, 0.0f));
    }
}
