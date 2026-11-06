package net.rai.scp_unleashed.scp610.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp610ClientEvents {
    private Scp610ClientEvents() {
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        for (Scp610Form Form : Scp610Form.values()) {
            EntityType<Scp610Creature> Type = (EntityType<Scp610Creature>) Scp610Registry.typeOf(Form);
            float Shadow = Math.min(1.6f, Form.width() * 0.6f);
            Event.registerEntityRenderer(Type, Context -> new Scp610Renderer<>(Context, Form, Form == Scp610Form.HiveLord ? 0.0f : Shadow));
        }
        Event.registerEntityRenderer(Scp610Registry.BileType.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        for (PlayerSkin.Model Skin : Event.getSkins()) {
            EntityRenderer<? extends Player> Found = Event.getSkin(Skin);
            if (Found instanceof PlayerRenderer Renderer) {
                Renderer.addLayer(new Scp610PlayerLayer(Renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp610Registry.SporeParticle.get(), Scp610Particles.SporeProvider::new);
        Event.registerSpriteSet(Scp610Registry.MawParticle.get(), Scp610Particles.MawProvider::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp610Veins.LayerId, Scp610Veins::render);
    }

    @SubscribeEvent
    public static void renderArm(RenderArmEvent Event) {
        AbstractClientPlayer User = Event.getPlayer();
        int Stage = Scp610ClientState.stage(User.getId());
        if (Stage <= 0 || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(User) instanceof PlayerRenderer Renderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> Model = Renderer.getModel();
        Model.attackTime = 0.0f;
        Model.crouching = false;
        Model.swimAmount = 0.0f;
        Model.setupAnim(User, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean Right = Event.getArm() == HumanoidArm.RIGHT;
        ModelPart Arm = Right ? Model.rightArm : Model.leftArm;
        ModelPart Sleeve = Right ? Model.rightSleeve : Model.leftSleeve;
        Arm.xRot = 0.0f;
        Sleeve.xRot = 0.0f;
        PoseStack Pose = Event.getPoseStack();
        VertexConsumer Buffer = Event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(Scp610PlayerLayer.texture(Stage)));
        Arm.render(Pose, Buffer, Event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        Sleeve.render(Pose, Buffer, Event.getPackedLight(), OverlayTexture.NO_OVERLAY);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp610Veins.tick();
    }

    @SubscribeEvent
    public static void leaveLevel(EntityLeaveLevelEvent Event) {
        if (Event.getLevel().isClientSide) {
            Scp610ClientState.forget(Event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp610ClientState.reset();
    }
}
