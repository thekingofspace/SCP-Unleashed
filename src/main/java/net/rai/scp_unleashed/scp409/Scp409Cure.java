package net.rai.scp_unleashed.scp409;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp409Cure {
    private Scp409Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (!Scp409Infection.isInfected(Target) && !Scp409Infection.hasModifiers(Target)) {
            return;
        }
        Event.markCured(Scp409Infection.cure(Target));
    }
}
