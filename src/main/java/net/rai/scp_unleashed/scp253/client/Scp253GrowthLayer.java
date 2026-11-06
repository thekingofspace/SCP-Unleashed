package net.rai.scp_unleashed.scp253.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp253.Scp253Display;
import net.rai.scp_unleashed.scp253.Scp253Growths;

import java.util.List;

public class Scp253GrowthLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public static final float[][] VillagerBounds = {
            {-4.0f, -10.0f, -4.0f, 4.0f, 0.0f, 4.0f},
            {-4.0f, 0.0f, -3.0f, 4.0f, 12.0f, 3.0f},
            {-8.0f, -2.0f, -2.0f, -4.0f, 6.0f, 2.0f},
            {4.0f, -2.0f, -2.0f, 8.0f, 6.0f, 2.0f},
            {-2.0f, 0.0f, -2.0f, 2.0f, 12.0f, 2.0f},
            {-2.0f, 0.0f, -2.0f, 2.0f, 12.0f, 2.0f}};
    public static final float VillagerHat = 0.51f;
    public static final float VillagerCoat = 0.5f;

    public Scp253GrowthLayer(RenderLayerParent<T, M> Parent) {
        super(Parent);
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int Light, T Owner, float LimbSwing, float LimbAmount, float Partial, float Age, float HeadYaw, float HeadPitch) {
        if (Owner.isInvisible()) {
            return;
        }
        Scp253Display Shown = Scp253GrowthPainter.display(Owner);
        if (Shown == null) {
            return;
        }
        float Progress = Scp253GrowthPainter.progress(Owner, Shown, Partial);
        if (!Scp253GrowthPainter.visible(Progress)) {
            return;
        }
        List<Scp253Growths.Growth> Layout = Scp253GrowthPainter.layout(Shown.Seed());
        int Overlay = LivingEntityRenderer.getOverlayCoords(Owner, 0.0f);
        M Model = getParentModel();
        if (Model instanceof HumanoidModel<?> Humanoid) {
            if (Humanoid.young) {
                return;
            }
            boolean Slim = Owner instanceof AbstractClientPlayer User && User.getSkin().model() == PlayerSkin.Model.SLIM;
            float[][] Bounds = Slim ? Scp253Growths.Slim : Scp253Growths.Wide;
            boolean Player = Owner instanceof AbstractClientPlayer;
            float[] Lifts = Scp253GrowthPainter.lifts(Owner, Player ? Scp253GrowthPainter.SkinLift : 0.0f, Player ? Scp253GrowthPainter.HatLift : 0.0f);
            ModelPart[] Parts = {Humanoid.head, Humanoid.body, Humanoid.rightArm, Humanoid.leftArm, Humanoid.rightLeg, Humanoid.leftLeg};
            for (int Part = 0; Part < Scp253Growths.Parts; Part++) {
                ModelPart Piece = Parts[Part];
                if (!Piece.visible) {
                    continue;
                }
                Pose.pushPose();
                Piece.translateAndRotate(Pose);
                Scp253GrowthPainter.paint(Pose, Buffers, Light, Overlay, Layout, Progress, Part, Bounds[Part], Lifts[Part]);
                Pose.popPose();
            }
        } else if (Model instanceof VillagerModel<?> Villager) {
            ModelPart Root = Villager.root();
            ModelPart[] Parts = {Villager.getHead(), Root.getChild("body"), Root.getChild("arms"), Root.getChild("arms"), Root.getChild("right_leg"), Root.getChild("left_leg")};
            float[] Lifts = {VillagerHat, VillagerCoat, 0.0f, 0.0f, VillagerCoat, VillagerCoat};
            for (int Part = 0; Part < Scp253Growths.Parts; Part++) {
                ModelPart Piece = Parts[Part];
                if (!Piece.visible) {
                    continue;
                }
                Pose.pushPose();
                Root.translateAndRotate(Pose);
                Piece.translateAndRotate(Pose);
                Scp253GrowthPainter.paint(Pose, Buffers, Light, Overlay, Layout, Progress, Part, VillagerBounds[Part], Lifts[Part]);
                Pose.popPose();
            }
        }
    }
}
