package net.rai.scp_unleashed.scp253.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.scp253.Scp253Display;
import net.rai.scp_unleashed.scp253.Scp253Growths;
import net.rai.scp_unleashed.scp253.Scp253Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp253ClientEvents {
    public static final float ArmBob = 0.1f;

    private Scp253ClientEvents() {
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        for (PlayerSkin.Model Skin : Event.getSkins()) {
            if (Event.getSkin(Skin) instanceof PlayerRenderer Renderer) {
                Renderer.addLayer(new Scp253GrowthLayer<>(Renderer));
            }
        }
        for (EntityType<?> Type : Event.getEntityTypes()) {
            EntityRenderer<?> Found = Event.getRenderer(Type);
            if (Found instanceof DClassRenderer Body) {
                Body.addRenderLayer(new Scp253GeoGrowthLayer(Body));
            } else if (Found instanceof LivingEntityRenderer Living && (Living.getModel() instanceof HumanoidModel || Living.getModel() instanceof VillagerModel)) {
                Living.addLayer(new Scp253GrowthLayer(Living));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void renderArm(RenderArmEvent Event) {
        AbstractClientPlayer User = Event.getPlayer();
        Scp253Display Shown = Scp253GrowthPainter.display(User);
        if (Shown == null) {
            return;
        }
        float Partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float Progress = Scp253GrowthPainter.progress(User, Shown, Partial);
        if (!Scp253GrowthPainter.visible(Progress)) {
            return;
        }
        boolean Right = Event.getArm() == HumanoidArm.RIGHT;
        int Part = Right ? Scp253Growths.RightArm : Scp253Growths.LeftArm;
        float[] Bounds = Scp253Growths.Wide[Part];
        PoseStack Pose = Event.getPoseStack();
        Pose.pushPose();
        if (Event.isCanceled() || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(User) instanceof PlayerRenderer Renderer)) {
            Pose.translate((Right ? -5.0f : 5.0f) / 16.0f, 2.0f / 16.0f, 0.0f);
            Pose.mulPose(Axis.ZP.rotation(Right ? ArmBob : -ArmBob));
        } else {
            PlayerModel<AbstractClientPlayer> Model = Renderer.getModel();
            Model.attackTime = 0.0f;
            Model.crouching = false;
            Model.swimAmount = 0.0f;
            Model.setupAnim(User, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
            ModelPart Arm = Right ? Model.rightArm : Model.leftArm;
            Arm.xRot = 0.0f;
            Arm.translateAndRotate(Pose);
            if (User.getSkin().model() == PlayerSkin.Model.SLIM) {
                Bounds = Scp253Growths.Slim[Part];
            }
        }
        Scp253GrowthPainter.paint(Pose, Event.getMultiBufferSource(), Event.getPackedLight(), OverlayTexture.NO_OVERLAY,
                Scp253GrowthPainter.layout(Shown.Seed()), Progress, Part, Bounds, Scp253GrowthPainter.SkinLift);
        Pose.popPose();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void steadyView(ComputeFovModifierEvent Event) {
        Player User = Event.getPlayer();
        AttributeInstance Speed = User.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeModifier Ours = Speed == null ? null : Speed.getModifier(Scp253Registry.SlownessId);
        float Walk = User.getAbilities().getWalkingSpeed();
        if (Ours == null || Walk == 0.0f || Ours.amount() <= -1.0) {
            return;
        }
        double Value = Speed.getValue();
        double Without = Value / (1.0 + Ours.amount());
        float Ratio = (float) ((Without / Walk + 1.0) / (Value / Walk + 1.0));
        if (!Float.isFinite(Ratio)) {
            return;
        }
        float Scale = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        float Raw = Event.getFovModifier();
        float Shift = Mth.lerp(Scale, 1.0f, Raw * Ratio) - Mth.lerp(Scale, 1.0f, Raw);
        Event.setNewFovModifier(Event.getNewFovModifier() + Shift);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp253GrowthPainter.forget();
    }
}
