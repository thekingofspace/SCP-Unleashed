package net.rai.scp_unleashed.fission.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.FissionReactorBlockEntity;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.GasDissolverBlockEntity;
import net.rai.scp_unleashed.fission.SonicVibratorBlockEntity;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasTankBlockEntity;
import net.rai.scp_unleashed.gas.VaporizerBlockEntity;
import net.rai.scp_unleashed.gas.client.GasClientEffects;
import net.rai.scp_unleashed.gas.client.VaporizerScreen;
import net.rai.scp_unleashed.plumbing.DrainBlockEntity;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class FissionClientEvents {
    private FissionClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        FissionReactorBlockEntity.setClientEffects(FissionClientEffects::reactorTick);
        SonicVibratorBlockEntity.setClientEffects(FissionClientEffects::vibratorTick);
        GasDissolverBlockEntity.setClientEffects(FissionClientEffects::dissolverTick);
        VaporizerBlockEntity.setClientEffects(GasClientEffects::vaporizerTick);
        DrainBlockEntity.setClientEffects(GasClientEffects::drainTick);
        Event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(FissionRegistry.WasteWater.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(FissionRegistry.FlowingWasteWater.get(), RenderType.translucent());
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(FissionRegistry.ReactorEntity.get(), FissionReactorRenderer::new);
        Event.registerBlockEntityRenderer(FissionRegistry.SonicVibratorEntity.get(), SonicVibratorRenderer::new);
        Event.registerEntityRenderer(FissionRegistry.ToxicGas.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(FissionRegistry.ReactorMenu.get(), FissionReactorScreen::new);
        Event.register(FissionRegistry.VibratorMenu.get(), SonicVibratorScreen::new);
        Event.register(FissionRegistry.DissolverMenu.get(), GasDissolverScreen::new);
        Event.register(GasRegistry.VaporizerMenuType.get(), VaporizerScreen::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(FissionRegistry.ToxicGasParticle.get(), GasPuffParticle.Provider::new);
        Event.registerSpriteSet(FissionRegistry.SonicRing.get(), SonicRingParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerFluidType(WasteWaterExtensions.Instance, FissionRegistry.WasteWaterType.get());
    }

    @SubscribeEvent
    public static void registerBlockColours(RegisterColorHandlersEvent.Block Event) {
        Event.register((State, World, Pos, TintIndex) -> TintIndex == 0 && World != null && Pos != null && World.getBlockEntity(Pos) instanceof GasTankBlockEntity Tank
                ? Tank.getClientColour() : GasTankBlockEntity.NoColour, GasRegistry.GasTank.get());
    }

    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.Item Event) {
        Event.register(new DynamicFluidContainerModel.Colors(), FissionRegistry.WasteWaterBucket.get());
    }
}
