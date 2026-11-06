package net.rai.scp_unleashed.scp129;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp129Cure {
    private Scp129Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        Event.markCured(clear(Event.target()));
    }

    public static boolean clear(LivingEntity Target) {
        Scp129Infection.State Current = Scp129Infection.stateOf(Target);
        boolean Infected = Current.Strains() != 0;
        if (!Infected && !Scp129Symptoms.episodeOf(Target).active()) {
            return false;
        }
        if (Infected) {
            Scp129Infection.store(Target, new Scp129Infection.State(0, Scp129Infection.Zeros, Current.Resisting(), 0));
            Scp129Course.clearEffects(Target);
        }
        Scp129Symptoms.interrupt(Target);
        if (Target instanceof ServerPlayer User) {
            Scp129Sync.send(User);
        }
        return true;
    }
}
