package net.rai.scp_unleashed.refinery.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.refinery.OrePurifierBlockEntity;
import net.rai.scp_unleashed.refinery.RefineryRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class RefineryClientEvents {
    private RefineryClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        OrePurifierBlockEntity.setClientEffects(PurifierClientEffects::tick);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(RefineryRegistry.OrePurifierMenuType.get(), OrePurifierScreen::new);
    }
}
