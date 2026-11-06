package net.rai.scp_unleashed.scp939;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.voice.VoiceArchive;
import net.rai.scp_unleashed.voice.VoicePolicy;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp939VoiceLure {
    public static final String Anomaly = VoicePolicy.Scp939;
    public static final double HearRange = 24.0;
    public static final int EarEvery = 10;
    public static final float ReplayChance = 0.85f;
    public static final float AdultScale = 1.0f;
    public static final float YoungScale = 1.25f;
    private static final Map<UUID, Ear> Ears = new ConcurrentHashMap<>();
    private static volatile boolean Active;

    private Scp939VoiceLure() {
    }

    public record Ear(UUID Hunter, ResourceKey<Level> Dimension, double X, double Y, double Z) {
    }

    public static boolean enabled() {
        return VoicePolicy.allows(Anomaly) && VoiceArchive.playback() != null;
    }

    public static boolean isListening() {
        return Active && !Ears.isEmpty();
    }

    public static int hear(VoiceArchive.Heard Sample, byte[] Frame, long NowMillis) {
        if (!Active || !VoiceArchive.capturable(Sample)) {
            return 0;
        }
        int Heard = 0;
        UUID First = null;
        for (Ear Each : Ears.values()) {
            if (!Each.Dimension().equals(Sample.Dimension())) {
                continue;
            }
            double Dx = Each.X() - Sample.X();
            double Dy = Each.Y() - Sample.Y();
            double Dz = Each.Z() - Sample.Z();
            if (Dx * Dx + Dy * Dy + Dz * Dz <= HearRange * HearRange) {
                Heard++;
                if (First == null) {
                    First = Each.Hunter();
                }
            }
        }
        if (First != null && !VoiceArchive.record(Anomaly, Sample, First, Frame, NowMillis)) {
            return 0;
        }
        return Heard;
    }

    public static void tick(MinecraftServer Server) {
        boolean Now = enabled();
        Active = Now;
        if (!Now) {
            Ears.clear();
        }
    }

    public static void listen(Scp939 Hunter) {
        if (Active && (Hunter.tickCount + Hunter.getId()) % EarEvery == 0) {
            ear(Hunter);
        }
    }

    public static void ear(Scp939 Hunter) {
        if (!Active) {
            return;
        }
        if (!Hunter.isAlive()) {
            forgetHunter(Hunter.getUUID());
            return;
        }
        Ears.put(Hunter.getUUID(), new Ear(Hunter.getUUID(), Hunter.level().dimension(), Hunter.getX(), Hunter.getEyeY(), Hunter.getZ()));
    }

    public static boolean call(ServerLevel World, Scp939 Hunter, @Nullable LivingEntity Prey) {
        if (!Active || Hunter.getRandom().nextFloat() >= ReplayChance) {
            return false;
        }
        return replay(World, Hunter, Prey);
    }

    public static boolean replay(ServerLevel World, Scp939 Hunter, @Nullable LivingEntity Prey) {
        if (!Active) {
            return false;
        }
        return VoiceArchive.replay(Anomaly, Hunter, Optional.empty(), Prey == null ? Optional.empty() : Optional.of(Prey.getUUID()), Hunter.isBaby() ? YoungScale : AdultScale);
    }

    public static void forgetHunter(UUID Hunter) {
        Ears.remove(Hunter);
    }

    public static int ears() {
        return Ears.size();
    }

    public static void clear() {
        Ears.clear();
        Active = false;
    }
}
