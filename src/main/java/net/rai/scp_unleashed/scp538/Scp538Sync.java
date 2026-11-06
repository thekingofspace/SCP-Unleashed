package net.rai.scp_unleashed.scp538;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp538.network.Scp538InfectionPayload;

public final class Scp538Sync {
    private Scp538Sync() {
    }

    public static Scp538InfectionPayload stateOf(LivingEntity Subject) {
        return new Scp538InfectionPayload(Scp538Infection.stage(Subject), Scp538Infection.progress(Subject));
    }

    public static void update(LivingEntity Subject) {
        if (!(Subject instanceof ServerPlayer User) || User.connection == null) {
            return;
        }
        Scp538Trace Trace = User.getData(Scp538Registry.Trace.get());
        Scp538InfectionPayload Payload = stateOf(User);
        if (Trace.SentStage == Payload.Stage() && Trace.SentProgress == Payload.Progress()) {
            return;
        }
        Trace.SentStage = Payload.Stage();
        Trace.SentProgress = Payload.Progress();
        PacketDistributor.sendToPlayer(User, Payload);
    }

    public static void resend(LivingEntity Subject) {
        if (!(Subject instanceof ServerPlayer User)) {
            return;
        }
        Scp538Trace Trace = User.getData(Scp538Registry.Trace.get());
        Trace.SentStage = -1;
        Trace.SentProgress = -1;
        update(User);
    }
}
