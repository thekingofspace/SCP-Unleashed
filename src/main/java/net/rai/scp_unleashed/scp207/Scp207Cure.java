package net.rai.scp_unleashed.scp207;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp207Cure {
    private Scp207Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        if (Event.target() instanceof ServerPlayer User && User.getExistingDataOrNull(Scp207Registry.Toll.get()) != null) {
            Scp207Toll.clear(User);
            Event.markCured();
        }
    }
}
