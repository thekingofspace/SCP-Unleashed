package net.rai.scp_unleashed.fusion.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fusion.FusionReactorBlockEntity;
import net.rai.scp_unleashed.fusion.FusionRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class FusionClientEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        FusionReactorBlockEntity.setClientEffects(FusionClientEffects::tick);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(FusionRegistry.ReactorMenu.get(), FusionScreen::new);
    }
}
