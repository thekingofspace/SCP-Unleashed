package net.rai.scp_unleashed.scp035.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp035.Scp035Host;
import net.rai.scp_unleashed.scp035.Scp035Registry;
import net.rai.scp_unleashed.scp035.Scp035Sight;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp035ClientEvents {
    public static final int WeepInterval = 4;
    public static final int WeepChance = 3;
    public static final double WeepRange = 24.0;
    public static final double FaceDepth = 0.3;
    public static final double EyeOffset = 0.09;

    private Scp035ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp035Display::register);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp035Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp035Registry.Mask.get(), Scp035MaskRenderer::new);
        Event.registerEntityRenderer(Scp035Registry.Host.get(), Scp035HostRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp035Registry.Weep.get(), Scp035WeepParticle.Provider::new);
        Event.registerSpriteSet(Scp035Registry.Glint.get(), Scp035GlintParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp035Overlay.MaskLayerId, Scp035Overlay::renderMask);
        Event.registerAbove(Scp035Overlay.MaskLayerId, Scp035Overlay.LureLayerId, Scp035Overlay::renderLure);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre Event) {
        Scp035ClientLure.frame(Event.getPartialTick());
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov Event) {
        if (Event.usedConfiguredFov() && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            Event.setFOV(Scp035ClientMask.fov(Event.getFOV(), (float) Event.getPartialTick()));
        }
    }

    @SubscribeEvent
    public static void turn(CalculatePlayerTurnEvent Event) {
        Event.setMouseSensitivity(Scp035ClientLure.sensitivity(Event.getMouseSensitivity()));
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Pre Event) {
        if (Event.getEntity() instanceof LocalPlayer User && User == Minecraft.getInstance().player) {
            Scp035ClientLure.drift(User);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp035ClientLure.tick(Client);
        Scp035ClientMask.tick(Client);
        ClientLevel World = Client.level;
        LocalPlayer User = Client.player;
        if (World == null || User == null || Client.isPaused()) {
            return;
        }
        boolean Weeping = World.getGameTime() % WeepInterval == 0L;
        for (LivingEntity Wearer : World.getEntitiesOfClass(LivingEntity.class, User.getBoundingBox().inflate(WeepRange), Scp035Sight::wearsMask)) {
            if (Wearer instanceof Scp035Host Host && Host.dummy() instanceof LivingEntity Dummy) {
                Scp035HostLook.stride(Host, Dummy);
            }
            if (Weeping && !(Wearer == User && Client.options.getCameraType().isFirstPerson())) {
                weep(World, Wearer);
            }
        }
    }

    private static void weep(ClientLevel World, LivingEntity Wearer) {
        RandomSource Random = World.random;
        if (Random.nextInt(WeepChance) != 0) {
            return;
        }
        Vec3 Look = Vec3.directionFromRotation(0.0f, Wearer.yHeadRot);
        Vec3 Side = new Vec3(-Look.z, 0.0, Look.x).scale(Random.nextBoolean() ? EyeOffset : -EyeOffset);
        Vec3 Eye = Wearer.getEyePosition().add(Look.scale(FaceDepth)).add(Side).add(0.0, -0.06, 0.0);
        World.addParticle(Scp035Registry.Weep.get(), Eye.x, Eye.y, Eye.z, Look.x * 0.01, 0.0, Look.z * 0.01);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp035ClientLure.reset();
        Scp035ClientMask.reset();
        Scp035Skins.reset();
    }
}
