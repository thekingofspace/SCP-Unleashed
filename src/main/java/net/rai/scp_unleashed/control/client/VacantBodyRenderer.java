package net.rai.scp_unleashed.control.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.control.VacantBody;

import java.util.UUID;

public class VacantBodyRenderer extends EntityRenderer<VacantBody> {
    private static final ResourceLocation White = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final float SeatDrop = 0.61f;
    private static final float Thigh = -1.5707964f;
    private static final float ArmY = 2.0f;
    private final PlayerModel<LivingEntity> Wide;
    private final PlayerModel<LivingEntity> Slim;

    public VacantBodyRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        Wide = new PlayerModel<>(Context.bakeLayer(ModelLayers.PLAYER), false);
        Slim = new PlayerModel<>(Context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        shadowRadius = 0.4f;
    }

    public static PlayerSkin skinOf(VacantBody Body) {
        UUID Owner = Body.getOwnerId();
        if (Owner == null) {
            return DefaultPlayerSkin.get(Body.getUUID());
        }
        Minecraft Game = Minecraft.getInstance();
        if (Game.getConnection() != null) {
            PlayerInfo Online = Game.getConnection().getPlayerInfo(Owner);
            if (Online != null) {
                return Online.getSkin();
            }
        }
        return DefaultPlayerSkin.get(Owner);
    }

    @Override
    public ResourceLocation getTextureLocation(VacantBody Body) {
        return skinOf(Body).texture();
    }

    @Override
    public void render(VacantBody Body, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        PlayerSkin Skin = skinOf(Body);
        PlayerModel<LivingEntity> Model = Skin.model() == PlayerSkin.Model.SLIM ? Slim : Wide;
        float Breath = Mth.sin((Body.tickCount + PartialTick) * 0.05f) * 0.02f;
        pose(Model, Body.isSeated(), Breath);
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Body.getYRot()));
        Pose.scale(-1.0f, -1.0f, 1.0f);
        Pose.translate(0.0f, -1.501f, 0.0f);
        if (Body.isSeated()) {
            Pose.translate(0.0f, SeatDrop, 0.0f);
        }
        VertexConsumer Consumer = Buffers.getBuffer(Model.renderType(Skin.texture()));
        Model.renderToBuffer(Pose, Consumer, Light, OverlayTexture.NO_OVERLAY);
        int[] Lids = VacantEyelids.colours(Skin.texture());
        if (Lids != null) {
            Pose.pushPose();
            Model.head.translateAndRotate(Pose);
            VertexConsumer Paint = Buffers.getBuffer(RenderType.entityCutoutNoCull(White));
            PoseStack.Pose Last = Pose.last();
            lid(Paint, Last, -3.0f, -1.0f, Lids[0], Lids[2], Light);
            lid(Paint, Last, 1.0f, 3.0f, Lids[1], Lids[3], Light);
            Pose.popPose();
        }
        Pose.popPose();
        super.render(Body, Yaw, PartialTick, Pose, Buffers, Light);
    }

    private static void pose(PlayerModel<LivingEntity> Model, boolean Seated, float Breath) {
        Model.young = false;
        Model.riding = false;
        Model.crouching = false;
        Model.attackTime = 0.0f;
        Model.swimAmount = 0.0f;
        Model.setAllVisible(true);
        Model.head.resetPose();
        Model.body.resetPose();
        Model.rightArm.resetPose();
        Model.leftArm.resetPose();
        Model.rightLeg.resetPose();
        Model.leftLeg.resetPose();
        Model.rightArm.y = ArmY;
        Model.leftArm.y = ArmY;
        Model.head.xRot = 0.62f + Breath;
        Model.head.zRot = 0.14f;
        Model.body.xRot = Seated ? 0.12f : 0.04f;
        Model.rightArm.xRot = Seated ? -0.42f : -0.08f;
        Model.rightArm.zRot = 0.12f;
        Model.leftArm.xRot = Seated ? -0.38f : -0.06f;
        Model.leftArm.zRot = -0.1f;
        if (Seated) {
            Model.rightLeg.xRot = Thigh;
            Model.rightLeg.yRot = 0.31415927f;
            Model.rightLeg.zRot = 0.07853982f;
            Model.leftLeg.xRot = Thigh;
            Model.leftLeg.yRot = -0.31415927f;
            Model.leftLeg.zRot = -0.07853982f;
        }
        Model.hat.copyFrom(Model.head);
        Model.jacket.copyFrom(Model.body);
        Model.rightSleeve.copyFrom(Model.rightArm);
        Model.leftSleeve.copyFrom(Model.leftArm);
        Model.rightPants.copyFrom(Model.rightLeg);
        Model.leftPants.copyFrom(Model.leftLeg);
    }

    private static void lid(VertexConsumer Paint, PoseStack.Pose Last, float FromX, float ToX, int Colour, int Lash, int Light) {
        float Z = -4.03f / 16.0f;
        float Top = -4.0f / 16.0f;
        float Middle = -3.25f / 16.0f;
        float Bottom = -3.0f / 16.0f;
        float X0 = FromX / 16.0f;
        float X1 = ToX / 16.0f;
        quad(Paint, Last, X0, Top, X1, Middle, Z, Colour, Light);
        quad(Paint, Last, X0, Middle, X1, Bottom, Z, Lash, Light);
    }

    private static void quad(VertexConsumer Paint, PoseStack.Pose Last, float X0, float Y0, float X1, float Y1, float Z, int Colour, int Light) {
        int Argb = 0xFF000000 | Colour;
        Paint.addVertex(Last, X0, Y0, Z).setColor(Argb).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, 0.0f, 0.0f, -1.0f);
        Paint.addVertex(Last, X0, Y1, Z).setColor(Argb).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, 0.0f, 0.0f, -1.0f);
        Paint.addVertex(Last, X1, Y1, Z).setColor(Argb).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, 0.0f, 0.0f, -1.0f);
        Paint.addVertex(Last, X1, Y0, Z).setColor(Argb).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, 0.0f, 0.0f, -1.0f);
    }
}
