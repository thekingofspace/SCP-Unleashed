package net.rai.scp_unleashed.scp294;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.scp294.network.Scp294RadiancePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp294Cure {
    private Scp294Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (!Scp294Ascension.isRising(Target)) {
            return;
        }
        Scp294Ascension.forget(Target.getUUID());
        if (Target instanceof ServerPlayer User) {
            PacketDistributor.sendToPlayer(User, new Scp294RadiancePayload(0));
        }
        Event.markCured();
    }
}
