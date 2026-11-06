package net.rai.scp_unleashed.guns.npc;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class NpcGunnerEvents {
    private NpcGunnerEvents() {
    }

    @SubscribeEvent
    public static void forgetGunner(EntityLeaveLevelEvent Event) {
        if (!Event.getLevel().isClientSide()) {
            NpcGunner.forget(Event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void forgetGunners(ServerStoppedEvent Event) {
        NpcGunner.forgetAll();
    }
}
