package net.rai.scp_unleashed.scp001day.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;

public class Scp001DayTurningLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation Wide = new ModelLayerLocation(Scp001DayRegistry.location("scp001_day_turning"), "wide");
    public static final ModelLayerLocation Slim = new ModelLayerLocation(Scp001DayRegistry.location("scp001_day_turning"), "slim");
    public static final float Inflate = 0.6f;
    public static final int Stages = 6;
    private static final ResourceLocation[] Textures = textures();
    private static PlayerModel<AbstractClientPlayer> WideShell;
    private static PlayerModel<AbstractClientPlayer> SlimShell;
    private final PlayerModel<AbstractClientPlayer> Shell;

    public Scp001DayTurningLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> Parent, EntityModelSet Models, boolean IsSlim) {
        super(Parent);
        Shell = new PlayerModel<>(Models.bakeLayer(IsSlim ? Slim : Wide), IsSlim);
        hideOuter(Shell);
        if (IsSlim) {
            SlimShell = Shell;
        } else {
            WideShell = Shell;
        }
    }

    private static ResourceLocation[] textures() {
        ResourceLocation[] Made = new ResourceLocation[Stages];
        for (int Stage = 0; Stage < Stages; Stage++) {
            Made[Stage] = Scp001DayRegistry.location("textures/entity/scp001day/turning_" + Stage + ".png");
        }
        return Made;
    }

    public static ResourceLocation texture(float Progress) {
        return Textures[Math.max(0, Math.min(Stages - 1, (int) (Progress * Stages)))];
    }

    private static void hideOuter(PlayerModel<AbstractClientPlayer> Model) {
        Model.hat.visible = false;
        Model.jacket.visible = false;
        Model.leftSleeve.visible = false;
        Model.rightSleeve.visible = false;
        Model.leftPants.visible = false;
        Model.rightPants.visible = false;
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int Light, AbstractClientPlayer Player, float LimbSwing, float LimbAmount, float Partial, float Age, float HeadYaw,
                       float HeadPitch) {
        float Turn = Scp001DayClientState.turning(Player, Partial);
        if (Turn < 0.0f || Player.isInvisible()) {
            return;
        }
        getParentModel().copyPropertiesTo(Shell);
        hideOuter(Shell);
        Shell.renderToBuffer(Pose, Buffers.getBuffer(RenderType.entityCutoutNoCull(texture(Turn))), Light, LivingEntityRenderer.getOverlayCoords(Player, 0.0f));
    }

    public static void arm(PoseStack Pose, MultiBufferSource Buffers, int Light, AbstractClientPlayer Player, HumanoidArm Side, float Turn) {
        PlayerModel<AbstractClientPlayer> Model = Player.getSkin().model() == PlayerSkin.Model.SLIM ? SlimShell : WideShell;
        if (Model == null) {
            return;
        }
        Model.attackTime = 0.0f;
        Model.crouching = false;
        Model.swimAmount = 0.0f;
        Model.setupAnim(Player, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        ModelPart Part = Side == HumanoidArm.RIGHT ? Model.rightArm : Model.leftArm;
        Part.xRot = 0.0f;
        Part.render(Pose, Buffers.getBuffer(RenderType.entityCutoutNoCull(texture(Turn))), Light, OverlayTexture.NO_OVERLAY);
    }
}
