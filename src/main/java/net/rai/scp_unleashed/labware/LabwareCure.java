package net.rai.scp_unleashed.labware;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class LabwareCure {
    private LabwareCure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (!LabContamination.isContaminated(Target)) {
            return;
        }
        LabContamination.clear(Target);
        Event.markCured();
    }
}
