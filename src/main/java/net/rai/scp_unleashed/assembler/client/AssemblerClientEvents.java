package net.rai.scp_unleashed.assembler.client;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.assembler.AdvancedAssemblerBlockEntity;
import net.rai.scp_unleashed.assembler.AssemblerRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class AssemblerClientEvents {
    private AssemblerClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        AdvancedAssemblerBlockEntity.setClientEffects(AssemblerClientEffects::tick);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(AssemblerRegistry.AssemblerMenuType.get(), AdvancedAssemblerScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(AssemblerRegistry.AdvancedAssemblerEntity.get(), AdvancedAssemblerRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(AssemblerRegistry.Spark.get(), AssemblerSparkParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ResourceLocation Location : AdvancedAssemblerRenderer.Models) {
            Event.register(ModelResourceLocation.standalone(Location));
        }
    }
}
