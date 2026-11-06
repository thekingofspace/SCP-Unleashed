package net.rai.scp_unleashed.scp409.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp409.Scp409Registry;
import software.bernie.geckolib.event.GeoRenderEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp409ClientEvents {
    private Scp409ClientEvents() {
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp409Crystals.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent Event) {
        Event.registerReloadListener((ResourceManagerReloadListener) Manager -> {
            Scp409Crystals.reload();
            Scp409Faces.clear();
            Scp409GeoFaces.clear();
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp409Registry.CaseEntity.get(), Scp409CaseRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp409Registry.ShardParticle.get(), Scp409ShardParticle.Provider::new);
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        Set<EntityRenderer<?>> Done = Collections.newSetFromMap(new IdentityHashMap<>());
        for (EntityType<?> Type : Event.getEntityTypes()) {
            EntityRenderer<?> Found = Event.getRenderer(Type);
            if (Found instanceof LivingEntityRenderer Renderer && Done.add(Found)) {
                Renderer.addLayer(new Scp409CrystalLayer<>(Renderer));
            }
        }
        for (PlayerSkin.Model Skin : Event.getSkins()) {
            EntityRenderer<? extends Player> Found = Event.getSkin(Skin);
            if (Found instanceof PlayerRenderer Renderer && Done.add(Found)) {
                Renderer.addLayer(new Scp409CrystalLayer<>(Renderer));
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void compileGeoLayers(GeoRenderEvent.Entity.CompileRenderLayers Event) {
        Event.addLayer(new Scp409GeoLayer(Event.getRenderer()));
    }

    @SubscribeEvent
    public static void hideShattered(RenderLivingEvent.Pre<?, ?> Event) {
        LivingEntity Owner = Event.getEntity();
        if (Owner.isDeadOrDying() && Scp409ClientState.isShattered(Owner.getId())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void hideShatteredGeo(GeoRenderEvent.Entity.Pre Event) {
        if (Event.getEntity() instanceof LivingEntity Owner && Owner.isDeadOrDying() && Scp409ClientState.isShattered(Owner.getId())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void renderArm(RenderArmEvent Event) {
        AbstractClientPlayer User = Event.getPlayer();
        Scp409ClientState.Entry Found = Scp409ClientState.get(User.getId());
        if (Found == null || !Found.visible() || !(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(User) instanceof PlayerRenderer Renderer)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> Model = Renderer.getModel();
        Model.attackTime = 0.0f;
        Model.crouching = false;
        Model.swimAmount = 0.0f;
        Model.setupAnim(User, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
        ModelPart Arm = Event.getArm() == HumanoidArm.RIGHT ? Model.rightArm : Model.leftArm;
        Arm.xRot = 0.0f;
        Scp409Faces Table = Scp409Faces.of((EntityModel<?>) Model);
        PoseStack Pose = Event.getPoseStack();
        Table.place(Pose, false, Table.anchors(User.getUUID()), Found.Growth(), Found.Stage(), Arm,
                (Where, Variant, Size) -> Scp409Crystals.cluster(Where, Event.getMultiBufferSource(), Event.getPackedLight(), Variant, Size, true));
    }

    @SubscribeEvent
    public static void leaveLevel(EntityLeaveLevelEvent Event) {
        if (Event.getLevel().isClientSide) {
            Scp409ClientState.forget(Event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp409ClientState.reset();
    }
}
