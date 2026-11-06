package net.rai.scp_unleashed.scp261;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp261Echo {
    public static final double HearRadius = 2.5;
    public static final int CaptureGap = 3;
    public static final int MaxQueue = 48;
    public static final int[] Delays = {9, 19};
    public static final float[] Volumes = {0.55f, 0.3f};
    public static final float[] Pitches = {0.95f, 0.9f};
    public static final double MinOffset = 3.0;
    public static final double MaxOffset = 6.0;
    private static final List<Pending> Queue = new ArrayList<>();
    private static final Map<UUID, Long> LastCapture = new HashMap<>();
    private static boolean Replaying;
    private static long Clock;

    private Scp261Echo() {
    }

    public static void heard(Level World, Holder<SoundEvent> Sound, SoundSource Source, double X, double Y, double Z, float Volume, float Pitch) {
        if (Replaying || !(World instanceof ServerLevel Server) || Sound == null || Queue.size() >= MaxQueue) {
            return;
        }
        for (ServerPlayer Listener : Server.players()) {
            if (!Listener.hasEffect(Scp261Registry.Echoing) || Listener.isSpectator()) {
                continue;
            }
            if (Listener.position().add(0.0, Listener.getBbHeight() * 0.5, 0.0).distanceToSqr(X, Y, Z) > HearRadius * HearRadius) {
                continue;
            }
            Long Last = LastCapture.get(Listener.getUUID());
            if (Last != null && Clock - Last < CaptureGap) {
                return;
            }
            LastCapture.put(Listener.getUUID(), Clock);
            RandomSource Random = Server.getRandom();
            double Turn = Random.nextDouble() * Math.PI * 2.0;
            for (int Index = 0; Index < Delays.length; Index++) {
                double Reach = MinOffset + Random.nextDouble() * (MaxOffset - MinOffset);
                double Angle = Turn + Index * Math.PI * (0.6 + Random.nextDouble() * 0.6);
                Vec3 Spot = new Vec3(X + Math.cos(Angle) * Reach, Y + Random.nextDouble() * 1.5, Z + Math.sin(Angle) * Reach);
                Queue.add(new Pending(Server.dimension(), Sound, Source, Spot, Volume * Volumes[Index], Pitch * Pitches[Index], Clock + Delays[Index]));
            }
            return;
        }
    }

    public static void tick(MinecraftServer Server) {
        Clock++;
        if (Queue.isEmpty()) {
            return;
        }
        Iterator<Pending> Walk = Queue.iterator();
        List<Pending> Due = new ArrayList<>();
        while (Walk.hasNext()) {
            Pending Next = Walk.next();
            if (Next.Due() <= Clock) {
                Due.add(Next);
                Walk.remove();
            }
        }
        Replaying = true;
        try {
            for (Pending Echo : Due) {
                ServerLevel World = Server.getLevel(Echo.Dimension());
                if (World != null) {
                    World.playSeededSound(null, Echo.Spot().x, Echo.Spot().y, Echo.Spot().z, Echo.Sound(), Echo.Source(), Echo.Volume(), Echo.Pitch(), World.getRandom().nextLong());
                }
            }
        } finally {
            Replaying = false;
        }
    }

    public static int pending() {
        return Queue.size();
    }

    public static void forget(UUID Listener) {
        LastCapture.remove(Listener);
    }

    public static void forgetAll() {
        Queue.clear();
        LastCapture.clear();
        Clock = 0L;
    }

    public record Pending(ResourceKey<Level> Dimension, Holder<SoundEvent> Sound, SoundSource Source, Vec3 Spot, float Volume, float Pitch, long Due) {
    }
}
