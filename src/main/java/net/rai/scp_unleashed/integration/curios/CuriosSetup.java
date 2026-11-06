package net.rai.scp_unleashed.integration.curios;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.senses.Senses;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class CuriosSetup {
    private CuriosSetup() {
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent Event) {
        if (Senses.curiosLoaded()) {
            CuriosSenses.register();
        }
    }
}
