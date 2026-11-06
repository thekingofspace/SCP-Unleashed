package net.rai.scp_unleashed.scp939;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp939VoiceEvents {
    private Scp939VoiceEvents() {
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp939VoiceLure.tick(Event.getServer());
        Scp939VoiceAttraction.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        if (Event.getEntity() instanceof Scp939 Hunter && Hunter.level() instanceof ServerLevel World) {
            Scp939VoiceLure.listen(Hunter);
            Scp939LureCadence.tick(World, Hunter, World.getGameTime());
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent Event) {
        if (Event.getEntity() instanceof Scp939 Hunter && !Event.getLevel().isClientSide()) {
            Scp939VoiceLure.forgetHunter(Hunter.getUUID());
            Scp939VoiceAttraction.forget(Hunter.getUUID());
            Scp939LureCadence.forget(Hunter);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp939VoiceLure.clear();
        Scp939VoiceAttraction.clear();
    }
}
