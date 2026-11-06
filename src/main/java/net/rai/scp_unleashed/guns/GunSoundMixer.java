package net.rai.scp_unleashed.guns;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class GunSoundMixer {
    public static final float MinAudible = 0.02F;
    public static final int MaxOverlap = 10;
    private static final Map<Level, List<Cue>> Pending = new WeakHashMap<>();
    private static final Set<Level> Ticking = Collections.newSetFromMap(new WeakHashMap<>());
    private static final List<Cue> Replaced = new ArrayList<>();
    private static final Map<UUID, Map<ResourceLocation, GunVoiceBudget<Cue>>> Voices = new HashMap<>();

    private GunSoundMixer() {
    }

    public static void emit(Level World, @Nullable Player Except, double X, double Y, double Z, SoundEvent Sound, SoundSource Source, float Volume, float Pitch) {
        Holder<SoundEvent> Wrapped = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(Sound);
        if (!(World instanceof ServerLevel Server)) {
            World.playSeededSound(Except, X, Y, Z, Wrapped, Source, Volume, Pitch, World.getRandom().nextLong());
            return;
        }
        PlayLevelSoundEvent.AtPosition Event = EventHooks.onPlaySoundAtPosition(Server, X, Y, Z, Wrapped, Source, Volume, Pitch);
        if (Event.isCanceled() || Event.getSound() == null) {
            return;
        }
        Cue Entry = new Cue(Except, X, Y, Z, Event.getSound(), Event.getSource(), Event.getNewVolume(), Event.getNewPitch(), Server.getRandom().nextLong());
        synchronized (Pending) {
            if (Ticking.contains(Server)) {
                Pending.computeIfAbsent(Server, Key -> new ArrayList<>()).add(Entry);
                return;
            }
        }
        deliver(Server, List.of(Entry));
    }

    public static void begin(Level World) {
        if (World instanceof ServerLevel Server) {
            synchronized (Pending) {
                Ticking.add(Server);
            }
        }
    }

    public static void emitAt(Level World, @Nullable Player Except, BlockPos Pos, SoundEvent Sound, SoundSource Source, float Volume, float Pitch) {
        emit(World, Except, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Sound, Source, Volume, Pitch);
    }

    public static void flush(Level World) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        List<Cue> Batch;
        synchronized (Pending) {
            Ticking.remove(Server);
            Batch = Pending.remove(Server);
        }
        if (Batch != null && !Batch.isEmpty()) {
            deliver(Server, Batch);
        }
    }

    private static void deliver(ServerLevel Server, List<Cue> Batch) {
        long Now = Server.getGameTime();
        List<Heard> Candidates = new ArrayList<>();
        for (ServerPlayer Listener : Server.getServer().getPlayerList().getPlayers()) {
            if (Listener.level().dimension() != Server.dimension()) {
                continue;
            }
            Candidates.clear();
            for (Cue Entry : Batch) {
                if (Listener == Entry.Except) {
                    continue;
                }
                double Dx = Entry.X - Listener.getX();
                double Dy = Entry.Y - Listener.getY();
                double Dz = Entry.Z - Listener.getZ();
                if (Dx * Dx + Dy * Dy + Dz * Dz >= Entry.Range * Entry.Range) {
                    continue;
                }
                float Gain = Entry.gainAt(Listener.getX(), Listener.getEyeY(), Listener.getZ());
                if (Entry.Known && Gain < MinAudible) {
                    continue;
                }
                Candidates.add(new Heard(Entry, Gain));
            }
            if (Candidates.isEmpty()) {
                continue;
            }
            Candidates.sort((First, Second) -> Float.compare(Second.Gain(), First.Gain()));
            Map<ResourceLocation, GunVoiceBudget<Cue>> Playing = Voices.computeIfAbsent(Listener.getUUID(), Key -> new HashMap<>());
            for (Heard Each : Candidates) {
                Cue Entry = Each.Entry();
                Replaced.clear();
                if (Playing.computeIfAbsent(Entry.Id, Key -> new GunVoiceBudget<>(MaxOverlap)).offer(Entry, Each.Gain(), Now, Entry.Ticks, Replaced)) {
                    Listener.connection.send(Entry.packet());
                }
            }
        }
    }

    public static void forget(UUID Listener) {
        Voices.remove(Listener);
    }

    public static void clear() {
        synchronized (Pending) {
            Pending.clear();
            Ticking.clear();
        }
        Voices.clear();
    }

    private record Heard(Cue Entry, float Gain) {
    }

    private static final class Cue {
        private final @Nullable Player Except;
        private final double X;
        private final double Y;
        private final double Z;
        private final Holder<SoundEvent> Sound;
        private final SoundSource Source;
        private final float Volume;
        private final float Pitch;
        private final long Seed;
        private final ResourceLocation Id;
        private final double Range;
        private final boolean Known;
        private final int Ticks;
        private @Nullable ClientboundSoundPacket Built;

        private Cue(@Nullable Player Except, double X, double Y, double Z, Holder<SoundEvent> Sound, SoundSource Source, float Volume, float Pitch, long Seed) {
            this.Except = Except;
            this.X = X;
            this.Y = Y;
            this.Z = Z;
            this.Sound = Sound;
            this.Source = Source;
            this.Volume = Volume;
            this.Pitch = Pitch;
            this.Seed = Seed;
            this.Id = Sound.value().getLocation();
            this.Range = Sound.value().getRange(Volume);
            this.Known = GunSoundLengths.known(Id);
            this.Ticks = GunSoundLengths.ticks(Id, Pitch);
        }

        private float gainAt(double Px, double Py, double Pz) {
            double Dx = X - Px;
            double Dy = Y - Py;
            double Dz = Z - Pz;
            double Reach = Math.max(1.0, Range);
            double Falloff = 1.0 - Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz) / Reach;
            return (float) (Volume * Math.max(0.0, Math.min(1.0, Falloff)));
        }

        private ClientboundSoundPacket packet() {
            if (Built == null) {
                Built = new ClientboundSoundPacket(Sound, Source, X, Y, Z, Volume, Pitch, Seed);
            }
            return Built;
        }
    }
}
