package net.rai.scp_unleashed.grenade;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class GrenadeCure {
    private GrenadeCure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        Event.markCured(GrenadeDaze.wake(Event.target()));
    }
}
