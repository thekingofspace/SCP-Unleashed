package net.rai.scp_unleashed.processing.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.processing.ProcessingMachineBlockEntity;
import net.rai.scp_unleashed.processing.ProcessingRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class ProcessingClientEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        ProcessingMachineBlockEntity.setClientHooks(ProcessingClientEffects::tick, ProcessingClientEffects::completed);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(ProcessingRegistry.ElectricFurnaceMenuType.get(), ElectricFurnaceScreen::new);
        Event.register(ProcessingRegistry.ItemPressMenuType.get(), ItemPressScreen::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(ItemPressRenderer.RamModel);
        Event.register(ItemPressRenderer.RodRamModel);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(ProcessingRegistry.ElectricFurnaceEntity.get(), ElectricFurnaceRenderer::new);
        Event.registerBlockEntityRenderer(ProcessingRegistry.ItemPressEntity.get(), ItemPressRenderer::new);
    }
}
