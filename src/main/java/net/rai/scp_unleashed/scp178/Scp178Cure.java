package net.rai.scp_unleashed.scp178;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp178Cure {
    private Scp178Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (Scp178Watchers.count(Target.getUUID()) > 0) {
            Event.markCured(Scp178Watchers.clear(Target) > 0);
        }
    }
}
