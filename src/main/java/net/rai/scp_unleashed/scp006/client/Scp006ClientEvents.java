package net.rai.scp_unleashed.scp006.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp006.Scp006Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp006ClientEvents {
    private Scp006ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(Scp006Registry.Water.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(Scp006Registry.FlowingWater.get(), RenderType.translucent());
            Scp006Display.register();
        });
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerFluidType(Scp006FluidExtensions.Instance, Scp006Registry.WaterType.get());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp006Registry.Glint.get(), Scp006GlintParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.Item Event) {
        Event.register(new DynamicFluidContainerModel.Colors(), Scp006Registry.Bucket.get());
    }
}
