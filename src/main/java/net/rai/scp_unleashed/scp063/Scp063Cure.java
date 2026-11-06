package net.rai.scp_unleashed.scp063;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp063Cure {
    private Scp063Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        if (!(Event.target() instanceof ServerPlayer User)) {
            return;
        }
        if (Scp063Teeth.isToothless(User)) {
            Event.markCured(Scp063Teeth.restore(User));
            return;
        }
        if (Scp063Brushing.brushTime(User) >= Scp063Brushing.StingTicks && !Scp063ToothbrushItem.isBrushingTeeth(User)) {
            Scp063Brushing.setBrushTime(User, 0);
            Event.markCured();
        }
    }
}
