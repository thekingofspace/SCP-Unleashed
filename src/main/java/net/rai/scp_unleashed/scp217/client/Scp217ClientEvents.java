package net.rai.scp_unleashed.scp217.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import software.bernie.geckolib.event.GeoRenderEvent;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp217ClientEvents {
    private Scp217ClientEvents() {
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp217Layout.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent Event) {
        Event.registerReloadListener((ResourceManagerReloadListener) Scp217Layout::reload);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        for (PlayerSkin.Model Skin : Event.getSkins()) {
            EntityRenderer<? extends Player> Found = Event.getSkin(Skin);
            if (Found instanceof PlayerRenderer Renderer) {
                Renderer.addLayer(new Scp217PlayerLayer(Renderer, Skin == PlayerSkin.Model.SLIM));
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void compileGeoLayers(GeoRenderEvent.Entity.CompileRenderLayers Event) {
        if (Event.getRenderer() instanceof DClassRenderer Renderer) {
            Event.addLayer(new Scp217GeoLayer(Renderer));
        }
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp217Tint.LayerId, Scp217Tint::render);
    }

    @SubscribeEvent
    public static void renderArm(RenderArmEvent Event) {
        AbstractClientPlayer User = Event.getPlayer();
        Scp217ClientState.Entry Found = Scp217ClientState.get(User.getId());
        if (Found == null || Found.stage() < Scp217ClockworkRenderer.FirstVisibleStage
                || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(User) instanceof PlayerRenderer Renderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> Model = Renderer.getModel();
        Model.attackTime = 0.0f;
        Model.crouching = false;
        Model.swimAmount = 0.0f;
        Model.setupAnim(User, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean Right = Event.getArm() == HumanoidArm.RIGHT;
        ModelPart Arm = Right ? Model.rightArm : Model.leftArm;
        Arm.xRot = 0.0f;
        PoseStack Pose = Event.getPoseStack();
        Pose.pushPose();
        Arm.translateAndRotate(Pose);
        Scp217ClockworkRenderer.draw(Pose, Event.getMultiBufferSource(), Event.getPackedLight(), User, Right ? "right_arm" : "left_arm", Found.stage(),
                Found.steps(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)), User.getSkin().model() == PlayerSkin.Model.SLIM, true, Found.perfect());
        Pose.popPose();
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        if (!Minecraft.getInstance().isPaused()) {
            Scp217ClientState.tick();
            Scp217PulseClient.tick();
        }
    }

    @SubscribeEvent
    public static void leaveLevel(EntityLeaveLevelEvent Event) {
        if (Event.getLevel().isClientSide) {
            Scp217ClientState.forget(Event.getEntity().getId());
            Scp217PulseClient.forget(Event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp217ClientState.reset();
        Scp217PulseClient.reset();
    }
}
