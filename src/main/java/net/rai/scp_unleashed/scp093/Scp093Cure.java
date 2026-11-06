package net.rai.scp_unleashed.scp093;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp093Cure {
    private Scp093Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (!Target.hasData(Scp093Registry.Doses)) {
            return;
        }
        Scp093Doses Current = Scp093Tears.read(Target);
        boolean Afflicted = Current.Amount() > 0.0f || Current.claimed() || Current.seeing(Target.level().getGameTime());
        Scp093Tears.write(Target, Scp093Doses.None);
        Scp093Visions.forget(Target);
        Event.markCured(Afflicted);
    }
}
