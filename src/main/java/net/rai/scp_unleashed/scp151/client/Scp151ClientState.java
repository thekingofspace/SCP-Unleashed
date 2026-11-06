package net.rai.scp_unleashed.scp151.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.rai.scp_unleashed.scp151.Scp151Breath;
import net.rai.scp_unleashed.scp151.network.Scp151BreathPayload;

public final class Scp151ClientState {
    private static boolean Gazing;
    private static boolean Recovering;
    private static long EndedAt;
    private static boolean Started;

    private Scp151ClientState() {
    }

    public static void apply(Scp151BreathPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        long Now = Client.level == null ? 0L : Client.level.getGameTime();
        if (Gazing && !Payload.Gazing()) {
            EndedAt = Now;
        }
        if (!Gazing && Payload.Gazing()) {
            Started = true;
        }
        Gazing = Payload.Gazing();
        Recovering = Payload.Recovering();
    }

    public static boolean isGazing() {
        return Gazing;
    }

    public static boolean isRecovering() {
        return Recovering;
    }

    public static boolean consumeStart() {
        boolean Was = Started;
        Started = false;
        return Was;
    }

    public static void breathe(LivingBreatheEvent Event, long Now) {
        if (!Gazing && !Recovering) {
            return;
        }
        long Since = Gazing ? 0L : Now - EndedAt + Scp151Breath.GazeHold;
        Scp151Breath.apply(Event, Gazing, Since, Now);
    }

    public static void reset() {
        Gazing = false;
        Recovering = false;
        EndedAt = 0L;
        Started = false;
    }
}
