package net.rai.scp_unleashed.sulfur.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class SulfurClientEvents {
    private SulfurClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(SulfurRegistry.Acid.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(SulfurRegistry.FlowingAcid.get(), RenderType.translucent());
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(SulfurRegistry.TankMenu.get(), IndustrialTankScreen::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerFluidType(SulfuricAcidExtensions.Instance, SulfurRegistry.AcidType.get());
    }

    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.Item Event) {
        Event.register(new DynamicFluidContainerModel.Colors(), SulfurRegistry.AcidBucket.get());
    }
}
