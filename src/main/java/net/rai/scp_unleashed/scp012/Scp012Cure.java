package net.rai.scp_unleashed.scp012;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp012Cure {
    private Scp012Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (!Scp012Composer.isComposer(Target)) {
            return;
        }
        if (Target instanceof ServerPlayer User) {
            Event.markCured(Scp012Composer.release(User));
            return;
        }
        Target.removeData(Scp012Registry.Composer.get());
        Event.markCured();
    }
}
