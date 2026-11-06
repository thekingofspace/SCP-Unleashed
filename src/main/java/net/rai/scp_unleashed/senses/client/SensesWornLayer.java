package net.rai.scp_unleashed.senses.client;

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
import net.rai.scp_unleashed.senses.Senses;
import net.rai.scp_unleashed.senses.SensesRegistry;

public class SensesWornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final ModelPart Blindfold;
    private final ModelPart Earplugs;

    public SensesWornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> Parent, EntityModelSet Models) {
        super(Parent);
        this.Blindfold = SensesWornModels.bake(Models, SensesWornModels.BlindfoldLayer);
        this.Earplugs = SensesWornModels.bake(Models, SensesWornModels.EarplugsLayer);
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int PackedLight, AbstractClientPlayer Wearer, float LimbSwing, float LimbSwingAmount,
                       float PartialTick, float AgeInTicks, float HeadYaw, float HeadPitch) {
        if (Wearer.isInvisible()) {
            return;
        }
        boolean Band = Senses.showsWorn(Wearer, SensesRegistry.Blindfold.get());
        boolean Plugs = Senses.showsWorn(Wearer, SensesRegistry.Earplugs.get());
        if (!Band && !Plugs) {
            return;
        }
        Pose.pushPose();
        getParentModel().getHead().translateAndRotate(Pose);
        if (Band) {
            Blindfold.render(Pose, Buffers.getBuffer(RenderType.entityCutoutNoCull(SensesWornModels.BlindfoldTexture)), PackedLight, OverlayTexture.NO_OVERLAY);
        }
        if (Plugs) {
            Earplugs.render(Pose, Buffers.getBuffer(RenderType.entityCutoutNoCull(SensesWornModels.EarplugsTexture)), PackedLight, OverlayTexture.NO_OVERLAY);
        }
        Pose.popPose();
    }
}
