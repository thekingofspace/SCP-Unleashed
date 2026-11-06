package net.rai.scp_unleashed.scp253;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp253Cure {
    private Scp253Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        Event.markCured(Scp253Infection.cure(Event.target()));
    }
}
