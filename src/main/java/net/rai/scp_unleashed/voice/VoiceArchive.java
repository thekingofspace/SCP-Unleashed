package net.rai.scp_unleashed.voice;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceArchive {
    public static final int DefaultCaptureTicks = 100;
    public static final long TickMillis = 50L;
    public static final float DefaultPitch = 1.0f;
    public static final int PruneEvery = 24000;
    private static final VoiceRecorder Recorder = new VoiceRecorder();
    private static final Map<UUID, Map<String, Long>> Sessions = new ConcurrentHashMap<>();
    private static final Map<String, VoiceStore> Stores = new HashMap<>();
    private static final Set<UUID> OptedOut = ConcurrentHashMap.newKeySet();
    private static final Random Roll = new Random();
    @Nullable
    private static volatile VoicePlayback Backend;
    @Nullable
    private static Path Root;
    private static long Ticks;

    private VoiceArchive() {
    }

    public record Heard(UUID Speaker, String Name, ResourceKey<Level> Dimension, double X, double Y, double Z, boolean Spectator, boolean Disabled,
                        boolean Whispering, boolean Grouped) {
    }

    public record Status(String Anomaly, int Clips, int Speakers, long Bytes) {
    }

    public static Path rootFor(MinecraftServer Server) {
        return Server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("scp_unleashed").resolve("voice");
    }

    public static Path optOutFile(Path VoiceRoot) {
        return VoiceRoot.resolveSibling(VoiceFiles.OptOutName);
    }

    public static void open(MinecraftServer Server) {
        Root = rootFor(Server);
        Stores.clear();
        Recorder.clear();
        Sessions.clear();
        OptedOut.clear();
        OptedOut.addAll(VoiceFiles.readOptOuts(optOutFile(Root)));
        Ticks = 0L;
        VoicePolicy.refresh();
    }

    public static void close() {
        if (Root != null && VoicePolicy.master()) {
            Recorder.sweep(Long.MAX_VALUE / 2);
            drain();
        }
        VoiceWriter.flush();
        Stores.clear();
        Recorder.clear();
        Sessions.clear();
        OptedOut.clear();
        Root = null;
    }

    @Nullable
    public static Path root() {
        return Root;
    }

    public static void setPlayback(@Nullable VoicePlayback Next) {
        VoicePlayback Old = Backend;
        Backend = Next;
        if (Old != null && Old != Next) {
            Old.stopAll();
        }
    }

    @Nullable
    public static VoicePlayback playback() {
        return Backend;
    }

    public static boolean capturable(Heard Sample) {
        return !Sample.Spectator() && !Sample.Disabled() && !Sample.Whispering() && !Sample.Grouped() && !VoiceGate.isSilenced(Sample.Speaker())
                && allowsSpeaker(Sample.Speaker());
    }

    public static boolean allowsSpeaker(UUID Speaker) {
        return !OptedOut.contains(Speaker);
    }

    public static void setAllowed(UUID Speaker, boolean Allow) {
        boolean Changed = Allow ? OptedOut.remove(Speaker) : OptedOut.add(Speaker);
        if (!Allow) {
            Sessions.remove(Speaker);
            Recorder.forgetSpeaker(Speaker);
        }
        if (Changed && Root != null) {
            Path Target = optOutFile(Root);
            List<UUID> Snapshot = new ArrayList<>(OptedOut);
            VoiceWriter.submit("save voice opt outs", () -> VoiceFiles.writeOptOuts(Target, Snapshot));
        }
    }

    public static boolean capture(String AnomalyId, ServerPlayer Speaker) {
        return capture(AnomalyId, Speaker, DefaultCaptureTicks);
    }

    public static boolean capture(String AnomalyId, ServerPlayer Speaker, int Ticks) {
        if (Ticks <= 0 || Speaker.isSpectator() || !allowsSpeaker(Speaker.getUUID()) || !VoicePolicy.allows(AnomalyId)) {
            return false;
        }
        long Until = System.currentTimeMillis() + Ticks * TickMillis;
        Sessions.computeIfAbsent(Speaker.getUUID(), Key -> new ConcurrentHashMap<>()).merge(AnomalyId, Until, Math::max);
        return true;
    }

    public static boolean isCapturing(String AnomalyId, UUID Speaker) {
        Map<String, Long> Mine = Sessions.get(Speaker);
        Long Until = Mine == null ? null : Mine.get(AnomalyId);
        return Until != null && Until >= System.currentTimeMillis();
    }

    public static void stopCapture(String AnomalyId, UUID Speaker) {
        Map<String, Long> Mine = Sessions.get(Speaker);
        if (Mine != null) {
            Mine.remove(AnomalyId);
        }
    }

    public static void stopAll(UUID Speaker) {
        Sessions.remove(Speaker);
    }

    public static int hear(Heard Sample, byte[] Frame, long NowMillis) {
        if (!VoicePolicy.masterCached() || !capturable(Sample)) {
            return 0;
        }
        Map<String, Long> Mine = Sessions.get(Sample.Speaker());
        if (Mine == null) {
            return 0;
        }
        int Count = 0;
        for (Map.Entry<String, Long> Entry : Mine.entrySet()) {
            if (Entry.getValue() >= NowMillis && VoicePolicy.allowsCached(Entry.getKey())
                    && Recorder.offer(Entry.getKey(), Sample.Speaker(), Sample.Name(), null, Frame, NowMillis)) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean record(String AnomalyId, Heard Sample, @Nullable UUID Listener, byte[] Frame, long NowMillis) {
        if (!VoicePolicy.allowsCached(AnomalyId) || !capturable(Sample)) {
            return false;
        }
        return Recorder.offer(AnomalyId, Sample.Speaker(), Sample.Name(), Listener, Frame, NowMillis);
    }

    public static void tick(MinecraftServer Server) {
        VoicePolicy.refresh();
        Ticks++;
        long Now = System.currentTimeMillis();
        for (Map<String, Long> Mine : Sessions.values()) {
            Mine.values().removeIf(Until -> Until < Now);
        }
        Sessions.values().removeIf(Map::isEmpty);
        if (!VoicePolicy.master()) {
            Recorder.clear();
            return;
        }
        Recorder.sweep(Now);
        drain();
        if (Ticks % PruneEvery == 0) {
            prune(Now);
        }
        VoicePlayback Current = Backend;
        if (Current != null) {
            Current.follow();
        }
    }

    public static int drain() {
        int Stored = 0;
        for (VoiceRecorder.Finished Each : Recorder.drain()) {
            if (Root != null && VoicePolicy.allows(Each.Anomaly())) {
                store(Each.Anomaly()).add(Each);
                Stored++;
            }
        }
        return Stored;
    }

    @Nullable
    public static VoiceClip accept(VoiceRecorder.Finished Made) {
        if (Root == null || !VoicePolicy.validId(Made.Anomaly())) {
            return null;
        }
        return store(Made.Anomaly()).add(Made);
    }

    public static VoiceRecorder recorder() {
        return Recorder;
    }

    public static VoiceStore store(String AnomalyId) {
        if (Root == null || !VoicePolicy.validId(AnomalyId)) {
            throw new IllegalStateException("The voice archive is not open or the id is bad: " + AnomalyId);
        }
        VoiceStore Found = Stores.computeIfAbsent(AnomalyId, Key -> new VoiceStore(Key, Root.resolve(Key)));
        Found.load(VoicePolicy.master(), System.currentTimeMillis());
        return Found;
    }

    public static List<String> anomalies() {
        TreeSet<String> Known = new TreeSet<>(Stores.keySet());
        if (Root != null) {
            Known.addAll(VoiceFiles.anomalies(Root));
        }
        return new ArrayList<>(Known);
    }

    public static boolean knows(String AnomalyId) {
        return anomalies().contains(AnomalyId);
    }

    public static int prune(long NowMillis) {
        if (Root == null) {
            return 0;
        }
        int Count = 0;
        for (String Anomaly : anomalies()) {
            Count += store(Anomaly).pruneAge(NowMillis);
        }
        return Count;
    }

    public static boolean replay(String AnomalyId, Entity Source, Optional<UUID> PreferredSpeaker) {
        return replay(AnomalyId, Source, PreferredSpeaker, Optional.empty(), DefaultPitch);
    }

    public static boolean replay(String AnomalyId, Entity Source, Optional<UUID> PreferredSpeaker, Optional<UUID> AvoidSpeaker, float PitchScale) {
        VoicePlayback Current = Backend;
        if (Current == null || Root == null || !VoicePolicy.allows(AnomalyId) || !(Source.level() instanceof ServerLevel World)) {
            return false;
        }
        VoiceStore Store = store(AnomalyId);
        List<VoiceClip> Allowed = new ArrayList<>();
        for (VoiceClip Each : Store.clips()) {
            if (allowsSpeaker(Each.Speaker())) {
                Allowed.add(Each);
            }
        }
        Optional<VoiceClip> Pick = choose(Allowed, PreferredSpeaker, AvoidSpeaker, Roll);
        if (Pick.isEmpty()) {
            return false;
        }
        List<byte[]> Frames = Store.frames(Pick.get().Id());
        if (Frames == null || Frames.isEmpty()) {
            return false;
        }
        return Current.play(World, Source, Frames, VoiceProfiles.forAnomaly(AnomalyId), PitchScale, Roll.nextLong());
    }

    public static Optional<VoiceClip> choose(List<VoiceClip> Clips, Optional<UUID> PreferredSpeaker, Optional<UUID> AvoidSpeaker, Random Dice) {
        if (Clips.isEmpty()) {
            return Optional.empty();
        }
        if (PreferredSpeaker.isPresent()) {
            List<VoiceClip> Own = new ArrayList<>();
            for (VoiceClip Each : Clips) {
                if (Each.Speaker().equals(PreferredSpeaker.get())) {
                    Own.add(Each);
                }
            }
            if (!Own.isEmpty()) {
                return Optional.of(Own.get(Dice.nextInt(Own.size())));
            }
        }
        List<VoiceClip> Pool = new ArrayList<>();
        for (VoiceClip Each : Clips) {
            if (AvoidSpeaker.isEmpty() || !Each.Speaker().equals(AvoidSpeaker.get())) {
                Pool.add(Each);
            }
        }
        List<VoiceClip> From = Pool.isEmpty() ? Clips : Pool;
        return Optional.of(From.get(Dice.nextInt(From.size())));
    }

    public static int clearAll() {
        if (Root == null) {
            return 0;
        }
        int Count = 0;
        for (String Anomaly : anomalies()) {
            Count += store(Anomaly).clear();
        }
        Recorder.clear();
        Path Target = Root;
        VoiceWriter.submit("clear every voice clip", () -> VoiceFiles.deleteTree(Target));
        return Count;
    }

    public static int clear(String AnomalyId) {
        if (Root == null || !VoicePolicy.validId(AnomalyId)) {
            return 0;
        }
        Recorder.forgetAnomaly(AnomalyId);
        return store(AnomalyId).clear();
    }

    public static int forget(UUID Speaker) {
        Recorder.forgetSpeaker(Speaker);
        Sessions.remove(Speaker);
        if (Root == null) {
            return 0;
        }
        int Count = 0;
        for (String Anomaly : anomalies()) {
            Count += store(Anomaly).removeSpeaker(Speaker);
        }
        return Count;
    }

    public static List<Status> status() {
        List<Status> Rows = new ArrayList<>();
        if (Root == null) {
            return Rows;
        }
        VoiceWriter.flush();
        for (String Anomaly : anomalies()) {
            VoiceStore Store = store(Anomaly);
            Rows.add(new Status(Anomaly, Store.size(), Store.speakers(), Store.diskBytes()));
        }
        return Rows;
    }

    public static void reload() {
        drain();
        VoiceWriter.flush();
        Stores.clear();
    }

    public static boolean flush() {
        return VoiceWriter.flush();
    }
}
