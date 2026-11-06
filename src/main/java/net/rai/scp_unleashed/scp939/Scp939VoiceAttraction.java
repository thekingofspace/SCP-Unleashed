package net.rai.scp_unleashed.scp939;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import net.rai.scp_unleashed.radio.VoiceConfig;
import net.rai.scp_unleashed.voice.VoiceArchive;
import net.rai.scp_unleashed.voice.VoicePolicy;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Scp939VoiceAttraction {
    public static final double SpeechRange = 32.0;
    public static final double WhisperRange = 6.0;
    public static final double HuntRange = 16.0;
    public static final long RefreshMillis = 1000L;
    public static final int MemoryTicks = 400;
    public static final int QueueCap = 256;
    public static final double ArriveDistance = 2.0;
    public static final int RepathTicks = 20;
    private static final Map<UUID, Long> LastHeard = new ConcurrentHashMap<>();
    private static final Queue<VoiceArchive.Heard> Pending = new ConcurrentLinkedQueue<>();
    private static final Map<UUID, Memory> Memories = new ConcurrentHashMap<>();
    private static volatile boolean Active;
    @Nullable
    private static volatile Boolean Forced;

    private Scp939VoiceAttraction() {
    }

    public record Memory(Vec3 Where, UUID Speaker, long Until) {
    }

    public static boolean enabled() {
        Boolean Override = Forced;
        return VoicePolicy.master() && (Override != null ? Override : VoiceConfig.voiceAttraction());
    }

    public static void force(@Nullable Boolean Value) {
        Forced = Value;
    }

    public static boolean isActive() {
        return Active;
    }

    public static boolean voiceHeard(VoiceArchive.Heard Sample, long NowMillis) {
        if (!Active || Sample.Spectator() || VoiceGate.isSilenced(Sample.Speaker())) {
            return false;
        }
        Long Last = LastHeard.get(Sample.Speaker());
        if (Last != null && NowMillis - Last < RefreshMillis) {
            return false;
        }
        if (Pending.size() >= QueueCap) {
            return false;
        }
        LastHeard.put(Sample.Speaker(), NowMillis);
        Pending.add(Sample);
        return true;
    }

    public static int tick(MinecraftServer Server) {
        boolean Now = enabled();
        Active = Now;
        if (!Now) {
            Pending.clear();
            LastHeard.clear();
            Memories.clear();
            return 0;
        }
        int Drawn = 0;
        VoiceArchive.Heard Sample;
        while ((Sample = Pending.poll()) != null) {
            ServerLevel World = Server.getLevel(Sample.Dimension());
            if (World != null) {
                Drawn += draw(World, Sample);
            }
        }
        for (ServerLevel World : Server.getAllLevels()) {
            long Time = World.getGameTime();
            Memories.values().removeIf(Each -> Each.Until() < Time - MemoryTicks * 4L);
        }
        return Drawn;
    }

    public static int draw(ServerLevel World, VoiceArchive.Heard Sample) {
        double Range = Sample.Whispering() ? WhisperRange : SpeechRange;
        Vec3 Where = new Vec3(Sample.X(), Sample.Y(), Sample.Z());
        long Until = World.getGameTime() + MemoryTicks;
        Player Speaker = World.getPlayerByUUID(Sample.Speaker());
        int Drawn = 0;
        for (Scp939 Hunter : World.getEntitiesOfClass(Scp939.class, AABB.ofSize(Where, Range * 2.0, Range * 2.0, Range * 2.0),
                Each -> Each.isAlive() && Each.position().distanceToSqr(Where) <= Range * Range)) {
            Memories.put(Hunter.getUUID(), new Memory(Where, Sample.Speaker(), Until));
            if (Speaker != null && Hunter.getTarget() == null && !Hunter.isBusy() && !Hunter.isLuring() && Hunter.distanceTo(Speaker) <= HuntRange
                    && Scp939Senses.isPrey(Hunter, Speaker) && (!Hunter.isContained() || Hunter.room() != null && Hunter.room().holds(Speaker.blockPosition()))) {
                Hunter.setTarget(Speaker);
            }
            Drawn++;
        }
        return Drawn;
    }

    @Nullable
    public static Memory remembered(Scp939 Hunter, long GameTime) {
        Memory Known = Memories.get(Hunter.getUUID());
        return Known != null && Known.Until() >= GameTime ? Known : null;
    }

    public static void forget(UUID Hunter) {
        Memories.remove(Hunter);
    }

    public static void forgetSpeaker(UUID Speaker) {
        LastHeard.remove(Speaker);
    }

    public static void clear() {
        Pending.clear();
        LastHeard.clear();
        Memories.clear();
        Active = false;
    }

    public static final class Investigate extends Goal {
        private final Scp939 Owner;
        private int Repath;

        public Investigate(Scp939 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Memory memory() {
            return remembered(Owner, Owner.level().getGameTime());
        }

        @Override
        public boolean canUse() {
            Memory Known = memory();
            return Known != null && Owner.getTarget() == null && !Owner.isBusy() && !Owner.isLuring() && !Owner.isBaby()
                    && Owner.position().distanceToSqr(Known.Where()) > ArriveDistance * ArriveDistance;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
            Owner.setProwling(true);
        }

        @Override
        public void stop() {
            Owner.setProwling(false);
            Owner.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Memory Known = memory();
            if (Known == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Known.Where().x, Known.Where().y, Known.Where().z, 20.0f, 20.0f);
            if (--Repath <= 0) {
                Repath = RepathTicks;
                Owner.getNavigation().moveTo(Known.Where().x, Known.Where().y, Known.Where().z, Scp939.ProwlModifier);
            }
        }
    }
}
