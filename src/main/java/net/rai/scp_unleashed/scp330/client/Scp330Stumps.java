package net.rai.scp_unleashed.scp330.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.rai.scp_unleashed.scp330.Scp330Registry;
import org.jetbrains.annotations.Nullable;

public final class Scp330Stumps {
    public static final ResourceLocation Texture = Scp330Registry.location("textures/entity/scp330/stump.png");
    public static final float Kept = 0.7f;
    public static final float ShoulderTop = 2.0f;
    public static final float ArmEnd = 10.0f;
    public static final float WrapHeight = 2.0f;
    @Nullable
    private static ModelPart Caps;

    private Scp330Stumps() {
    }

    public static boolean isGreedy(Entity Shown) {
        return Scp330ClientState.isGreedy(Shown);
    }

    public static void shorten(ModelPart Arm, ModelPart Sleeve) {
        Arm.yScale = Kept;
        Arm.y -= ShoulderTop * (1.0f - Kept);
        Sleeve.copyFrom(Arm);
    }

    public static void restore(ModelPart Arm, ModelPart Sleeve) {
        Arm.yScale = 1.0f;
        Sleeve.yScale = 1.0f;
    }

    private static ModelPart capModel() {
        if (Caps == null) {
            MeshDefinition Mesh = new MeshDefinition();
            PartDefinition Root = Mesh.getRoot();
            CubeDeformation Wrap = new CubeDeformation(0.35f, 0.3f, 0.35f);
            Root.addOrReplaceChild("right_wide", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -WrapHeight, -2.0f, 4.0f, WrapHeight, 4.0f, Wrap), PartPose.ZERO);
            Root.addOrReplaceChild("left_wide", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -WrapHeight, -2.0f, 4.0f, WrapHeight, 4.0f, Wrap), PartPose.ZERO);
            Root.addOrReplaceChild("right_slim", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0f, -WrapHeight, -2.0f, 3.0f, WrapHeight, 4.0f, Wrap), PartPose.ZERO);
            Root.addOrReplaceChild("left_slim", CubeListBuilder.create().texOffs(0, 8).addBox(-1.0f, -WrapHeight, -2.0f, 3.0f, WrapHeight, 4.0f, Wrap), PartPose.ZERO);
            Caps = LayerDefinition.create(Mesh, 32, 16).bakeRoot();
        }
        return Caps;
    }

    public static boolean slim(AbstractClientPlayer User) {
        return User.getSkin().model() == PlayerSkin.Model.SLIM;
    }

    public static void cap(PoseStack Pose, VertexConsumer Buffer, int Light, int Overlay, ModelPart Arm, boolean Right, boolean Slim) {
        if (!Arm.visible) {
            return;
        }
        ModelPart Part = capModel().getChild((Right ? "right_" : "left_") + (Slim ? "slim" : "wide"));
        Pose.pushPose();
        Arm.translateAndRotate(Pose);
        Pose.translate(0.0f, ArmEnd / 16.0f, 0.0f);
        Pose.scale(1.0f, 1.0f / Math.max(0.05f, Arm.yScale), 1.0f);
        Part.render(Pose, Buffer, Light, Overlay);
        Pose.popPose();
    }

    public static void caps(PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay, PlayerModel<AbstractClientPlayer> Model, AbstractClientPlayer User) {
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.entityCutoutNoCull(Texture));
        boolean Slim = slim(User);
        cap(Pose, Buffer, Light, Overlay, Model.rightArm, true, Slim);
        cap(Pose, Buffer, Light, Overlay, Model.leftArm, false, Slim);
    }

    public static void firstPerson(PoseStack Pose, MultiBufferSource Buffers, int Light, AbstractClientPlayer User, PlayerModel<AbstractClientPlayer> Model, HumanoidArm Side) {
        boolean Right = Side == HumanoidArm.RIGHT;
        ModelPart Arm = Right ? Model.rightArm : Model.leftArm;
        ModelPart Sleeve = Right ? Model.rightSleeve : Model.leftSleeve;
        Model.attackTime = 0.0f;
        Model.crouching = false;
        Model.swimAmount = 0.0f;
        Model.setupAnim(User, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        ResourceLocation Skin = User.getSkin().texture();
        Arm.xRot = 0.0f;
        Arm.render(Pose, Buffers.getBuffer(RenderType.entitySolid(Skin)), Light, OverlayTexture.NO_OVERLAY);
        Sleeve.xRot = 0.0f;
        Sleeve.render(Pose, Buffers.getBuffer(RenderType.entityTranslucent(Skin)), Light, OverlayTexture.NO_OVERLAY);
        cap(Pose, Buffers.getBuffer(RenderType.entityCutoutNoCull(Texture)), Light, OverlayTexture.NO_OVERLAY, Arm, Right, slim(User));
    }
}
