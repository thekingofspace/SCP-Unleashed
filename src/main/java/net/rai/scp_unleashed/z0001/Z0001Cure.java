package net.rai.scp_unleashed.z0001;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Z0001Cure {
    private Z0001Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        if (Event.target() instanceof ServerPlayer User) {
            Event.markCured(Z0001Ink.forget(User.getUUID()));
        }
    }
}
