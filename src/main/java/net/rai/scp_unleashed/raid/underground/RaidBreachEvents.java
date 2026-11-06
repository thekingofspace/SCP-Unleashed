package net.rai.scp_unleashed.raid.underground;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidWorld;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class RaidBreachEvents {
    private static final Logger Log = LogUtils.getLogger();

    private RaidBreachEvents() {
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        long Now = Server.overworld().getGameTime();
        for (Raid Current : RaidWorld.get(Server).snapshot()) {
            if (!Current.live() || RaidBreach.mode(Current) != RaidBreach.ModeShaft) {
                continue;
            }
            byte Phase = RaidBreach.phase(Current);
            if (Phase != RaidBreach.PhaseArmed && Phase != RaidBreach.PhaseBlasting) {
                continue;
            }
            ServerLevel World = Server.getLevel(Current.dimension());
            if (World == null) {
                continue;
            }
            try {
                RaidBreach.tick(World, Current, Now);
            } catch (RuntimeException Problem) {
                Log.error("Raid {} breach failed to tick", Current.id(), Problem);
                RaidBreach.fail(World, Current, "error");
            }
        }
    }
}
