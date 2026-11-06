package net.rai.scp_unleashed.scp079;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp079Cure {
    private Scp079Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        boolean Calmed = Scp079Exposure.calm(Target);
        if (!Target.hasData(Scp079Registry.Exposure)) {
            Event.markCured(Calmed);
            return;
        }
        boolean Felt = Scp079Exposure.stage(Scp079Exposure.exposure(Target)) > 0;
        Scp079Exposure.clear(Target);
        Event.markCured(Calmed || Felt);
    }
}
