package net.rai.scp_unleashed.oil.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.oil.OilLiquid;
import net.rai.scp_unleashed.oil.OilMachineBlockEntity;
import net.rai.scp_unleashed.oil.OilRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class OilClientEvents {
    private OilClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        OilMachineBlockEntity.setClientEffects(OilClientEffects::tick);
        Event.enqueueWork(() -> {
            for (OilLiquid Liquid : OilRegistry.Liquids) {
                ItemBlockRenderTypes.setRenderLayer(Liquid.getSource(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(Liquid.getFlowing(), RenderType.translucent());
            }
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(OilRegistry.OilExtractorEntity.get(), OilExtractorRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : OilExtractorRenderer.Parts) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(OilRegistry.ExtractorMenu.get(), OilExtractorScreen::new);
        Event.register(OilRegistry.TowerMenu.get(), DistillationTowerScreen::new);
        Event.register(OilRegistry.ProcessorMenu.get(), ChemicalProcessorScreen::new);
        Event.register(OilRegistry.GeneratorMenu.get(), CombustionGeneratorScreen::new);
        Event.register(OilRegistry.RefineryMenu.get(), OilRefineryScreen::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent Event) {
        for (OilLiquid Liquid : OilRegistry.Liquids) {
            Event.registerFluidType(new OilFluidExtensions(Liquid.getGrade()), Liquid.getType().get());
        }
    }

    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.Item Event) {
        for (OilLiquid Liquid : OilRegistry.Liquids) {
            Event.register(new DynamicFluidContainerModel.Colors(), Liquid.getBucket().get());
        }
    }
}
