package net.rai.scp_unleashed.scp224;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp224Cure {
    private Scp224Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (!Target.hasData(Scp224Registry.Age)) {
            return;
        }
        Scp224Aging.clear(Target);
        if (Target instanceof ServerPlayer User) {
            Scp224Aging.sync(User);
        }
        Event.markCured();
    }
}
