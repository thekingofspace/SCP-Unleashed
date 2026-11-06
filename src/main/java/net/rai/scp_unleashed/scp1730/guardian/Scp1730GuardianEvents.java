package net.rai.scp_unleashed.scp1730.guardian;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp1730GuardianEvents {
    private Scp1730GuardianEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp1730GuardianRegistry.ContainedType.get(), Scp1730Guardian.createAttributes().build());
    }
}
