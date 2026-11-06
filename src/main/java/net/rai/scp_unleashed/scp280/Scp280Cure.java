package net.rai.scp_unleashed.scp280;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.scp280.network.Scp280EyesPayload;
import net.rai.scp_unleashed.scp280.network.Scp280MeterPayload;

import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp280Cure {
    private Scp280Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        if (!(Event.target() instanceof ServerPlayer User)) {
            return;
        }
        Scp280World Data = Scp280World.get(User.server);
        UUID Id = User.getUUID();
        if (Data.meter(Id) <= 0.0f) {
            return;
        }
        Data.setMeter(Id, 0.0f);
        Scp280Haunt.forget(Id);
        if (!(User instanceof FakePlayer)) {
            Scp280Haunt.send(User, Scp280EyesPayload.clear());
            Scp280Haunt.sync(User, Scp280Haunt.haunt(Id), 0.0f, true, Scp280MeterPayload.Cleared);
        }
        Event.markCured();
    }
}
