package net.rai.scp_unleashed.scp217.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class Scp217PlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final boolean Slim;

    public Scp217PlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> Parent, boolean Slim) {
        super(Parent);
        this.Slim = Slim;
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int Light, AbstractClientPlayer Owner, float LimbSwing, float LimbAmount, float Partial, float Age,
                       float HeadYaw, float HeadPitch) {
        Scp217ClientState.Entry Found = Scp217ClientState.get(Owner.getId());
        if (Found == null || Found.stage() < Scp217ClockworkRenderer.FirstVisibleStage || Owner.isInvisible() || Owner.isSpectator()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> Model = getParentModel();
        float Steps = Found.steps(Partial);
        part(Pose, Buffers, Light, Owner, Model.head, "head", Found, Steps);
        part(Pose, Buffers, Light, Owner, Model.body, "body", Found, Steps);
        part(Pose, Buffers, Light, Owner, Model.rightArm, "right_arm", Found, Steps);
        part(Pose, Buffers, Light, Owner, Model.leftArm, "left_arm", Found, Steps);
        part(Pose, Buffers, Light, Owner, Model.rightLeg, "right_leg", Found, Steps);
        part(Pose, Buffers, Light, Owner, Model.leftLeg, "left_leg", Found, Steps);
    }

    private void part(PoseStack Pose, MultiBufferSource Buffers, int Light, AbstractClientPlayer Owner, ModelPart Part, String Name, Scp217ClientState.Entry Found, float Steps) {
        if (!Part.visible) {
            return;
        }
        Pose.pushPose();
        Part.translateAndRotate(Pose);
        Scp217ClockworkRenderer.draw(Pose, Buffers, Light, Owner, Name, Found.stage(), Steps, Slim, false, Found.perfect());
        Pose.popPose();
    }
}
