package net.rai.scp_unleashed.scp354.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp354.Scp354Lion;
import net.rai.scp_unleashed.scp354.Scp354Lizard;
import net.rai.scp_unleashed.scp354.Scp354Registry;
import net.rai.scp_unleashed.scp354.Scp354Tentacle;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp354ClientEvents {
    public static final float TentacleShadow = 0.9f;
    public static final float LionShadow = 1.0f;
    public static final float LizardShadow = 1.2f;

    private Scp354ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(Scp354Registry.Red.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(Scp354Registry.RedFlowing.get(), RenderType.translucent());
        });
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerFluidType(Scp354FluidExtensions.Instance, Scp354Registry.RedType.get());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp354Registry.Grasp.get(), Scp354GraspRenderer::new);
        Event.registerEntityRenderer(Scp354Registry.Tentacle.get(), Context -> new Scp354BeastRenderer<Scp354Tentacle>(Context,
                new Scp354GeoModel<>("scp354_tentacle", "body", "emerge", Scp354Tentacle::isEmerging), TentacleShadow));
        Event.registerEntityRenderer(Scp354Registry.Lion.get(), Context -> new Scp354BeastRenderer<Scp354Lion>(Context,
                new Scp354GeoModel<>("scp354_lion", "body", "emerge", Scp354Lion::isEmerging), LionShadow));
        Event.registerEntityRenderer(Scp354Registry.Lizard.get(), Context -> new Scp354BeastRenderer<Scp354Lizard>(Context,
                new Scp354GeoModel<>("scp354_lizard", "body", "emerge", Scp354Lizard::isEmerging), LizardShadow));
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp354Registry.Bubble.get(), Scp354Particles.BubbleProvider::new);
        Event.registerSpriteSet(Scp354Registry.Ripple.get(), Scp354Particles.RippleProvider::new);
        Event.registerSpriteSet(Scp354Registry.Drip.get(), Scp354Particles.DripProvider::new);
        Event.registerSpriteSet(Scp354Registry.Splash.get(), Scp354Particles.SplashProvider::new);
    }
}
