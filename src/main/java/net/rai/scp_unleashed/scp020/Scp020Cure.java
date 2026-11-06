package net.rai.scp_unleashed.scp020;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp020Cure {
    private Scp020Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (Scp020Affliction.isAfflicted(Target)) {
            Event.markCured(Scp020Affliction.cure(Target));
        }
    }
}
