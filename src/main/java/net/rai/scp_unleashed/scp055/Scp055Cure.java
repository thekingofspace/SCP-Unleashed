package net.rai.scp_unleashed.scp055;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp055Cure {
    private Scp055Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        if (Event.target() instanceof ServerPlayer User && Scp055Episodes.isActive(User)) {
            Scp055Episodes.interrupt(User);
            Event.markCured();
        }
    }
}
