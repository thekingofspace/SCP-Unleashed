package net.rai.scp_unleashed.scp008;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp008Cure {
    private Scp008Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        Event.markCured(Scp008Infection.cure(Event.target()));
    }
}
