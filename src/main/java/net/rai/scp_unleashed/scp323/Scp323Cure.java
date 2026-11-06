package net.rai.scp_unleashed.scp323;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp323Cure {
    private Scp323Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        if (Scp323Bond.isHorned(Event.target())) {
            Event.markCured(Scp323Hosts.cure(Event.target()));
        }
    }
}
