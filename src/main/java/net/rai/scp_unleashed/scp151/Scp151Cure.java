package net.rai.scp_unleashed.scp151;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp151Cure {
    private Scp151Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (!Scp151Breath.isRecovering(Target)) {
            return;
        }
        Target.removeData(Scp151Registry.LastGaze);
        if (Target instanceof ServerPlayer User) {
            Scp151Breath.sync(User);
        }
        Event.markCured();
    }
}
