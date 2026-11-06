package net.rai.scp_unleashed.scp427;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp427Cure {
    private Scp427Cure() {
    }

    public static boolean ease(LivingEntity Target) {
        Scp427State State = Scp427Exposure.state(Target);
        if (State == null || State.transforming() || State.Ticks <= 0) {
            return false;
        }
        Scp427Exposure.setExposure(Target, State.Ticks / 2);
        return true;
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        Event.markCured(ease(Event.target()));
    }
}
