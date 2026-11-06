package net.rai.scp_unleashed.reactor.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.reactor.ReactorRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ReactorClientEvents {
    private ReactorClientEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(ReactorRegistry.ControllerMenu.get(), ReactorControllerScreen::new);
        Event.register(ReactorRegistry.ChamberMenu.get(), IrradiationChamberScreen::new);
    }
}
