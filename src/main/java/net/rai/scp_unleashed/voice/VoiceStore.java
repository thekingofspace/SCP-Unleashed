package net.rai.scp_unleashed.voice;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class VoiceStore {
    public static final int CacheSize = 16;
    private final String Anomaly;
    private final Path Folder;
    private final LinkedHashMap<UUID, VoiceClip> Clips = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, List<byte[]>> Cache = new LinkedHashMap<>(CacheSize, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, List<byte[]>> Eldest) {
            return size() > CacheSize;
        }
    };
    private boolean Loaded;

    public VoiceStore(String Anomaly, Path Folder) {
        this.Anomaly = Anomaly;
        this.Folder = Folder;
    }

    public String anomaly() {
        return Anomaly;
    }

    public Path folder() {
        return Folder;
    }

    public boolean isLoaded() {
        return Loaded;
    }

    public void load(boolean Prune, long NowMillis) {
        if (Loaded) {
            return;
        }
        Loaded = true;
        List<VoiceClip> Index = VoiceFiles.readIndex(Folder, Anomaly);
        boolean Rebuilt = false;
        if (Index == null) {
            Index = VoiceFiles.rebuild(Folder, Anomaly);
            Rebuilt = !Index.isEmpty();
        }
        Index.sort(Comparator.comparingLong(VoiceClip::Recorded));
        for (VoiceClip Each : Index) {
            Clips.put(Each.Id(), Each);
        }
        if (Rebuilt) {
            saveIndex();
        }
        if (Prune) {
            pruneAge(NowMillis);
        }
    }

    public List<VoiceClip> clips() {
        return new ArrayList<>(Clips.values());
    }

    public int size() {
        return Clips.size();
    }

    public int speakers() {
        Set<UUID> Seen = new HashSet<>();
        for (VoiceClip Each : Clips.values()) {
            Seen.add(Each.Speaker());
        }
        return Seen.size();
    }

    public long diskBytes() {
        return VoiceFiles.folderBytes(Folder);
    }

    public VoiceClip add(VoiceRecorder.Finished Made) {
        int Bytes = 0;
        for (byte[] Frame : Made.Frames()) {
            Bytes += Frame.length;
        }
        VoiceClip Meta = new VoiceClip(UUID.randomUUID(), Anomaly, Made.Speaker(), Made.SpeakerName(), Made.Recorded(), Made.Listener(), Made.Frames().size(), Bytes);
        Clips.put(Meta.Id(), Meta);
        Cache.put(Meta.Id(), Made.Frames());
        List<byte[]> Frames = Made.Frames();
        VoiceWriter.submit("save a voice clip", () -> VoiceFiles.writeClip(Folder, Meta, Frames));
        List<UUID> Evicted = overCap();
        drop(Evicted, false);
        saveIndex();
        return Meta;
    }

    private List<UUID> overCap() {
        List<UUID> Evicted = new ArrayList<>();
        int PerSpeaker = Math.max(1, VoicePolicy.clipsPerSpeaker());
        Map<UUID, List<VoiceClip>> BySpeaker = new LinkedHashMap<>();
        for (VoiceClip Each : sorted()) {
            BySpeaker.computeIfAbsent(Each.Speaker(), Key -> new ArrayList<>()).add(Each);
        }
        for (List<VoiceClip> Own : BySpeaker.values()) {
            for (int Index = 0; Index < Own.size() - PerSpeaker; Index++) {
                Evicted.add(Own.get(Index).Id());
            }
        }
        int PerAnomaly = Math.max(1, VoicePolicy.clipsPerAnomaly());
        List<VoiceClip> Left = new ArrayList<>();
        for (VoiceClip Each : sorted()) {
            if (!Evicted.contains(Each.Id())) {
                Left.add(Each);
            }
        }
        for (int Index = 0; Index < Left.size() - PerAnomaly; Index++) {
            Evicted.add(Left.get(Index).Id());
        }
        return Evicted;
    }

    private List<VoiceClip> sorted() {
        List<VoiceClip> Ordered = new ArrayList<>(Clips.values());
        Ordered.sort(Comparator.comparingLong(VoiceClip::Recorded));
        return Ordered;
    }

    public int pruneAge(long NowMillis) {
        long MaxAge = VoicePolicy.maxAgeMillis();
        return removeWhere(Each -> Each.olderThan(NowMillis, MaxAge));
    }

    public int removeSpeaker(UUID Speaker) {
        return removeWhere(Each -> Each.Speaker().equals(Speaker));
    }

    public int removeWhere(Predicate<VoiceClip> Test) {
        List<UUID> Gone = new ArrayList<>();
        for (VoiceClip Each : Clips.values()) {
            if (Test.test(Each)) {
                Gone.add(Each.Id());
            }
        }
        drop(Gone, true);
        return Gone.size();
    }

    private void drop(Collection<UUID> Gone, boolean SaveIndex) {
        if (Gone.isEmpty()) {
            return;
        }
        for (UUID Id : Gone) {
            Clips.remove(Id);
            Cache.remove(Id);
            Path File = VoiceFiles.clipPath(Folder, Id);
            VoiceWriter.submit("delete a voice clip", () -> Files.deleteIfExists(File));
        }
        if (SaveIndex) {
            saveIndex();
        }
    }

    public int clear() {
        int Count = Clips.size();
        Clips.clear();
        Cache.clear();
        Loaded = true;
        Path Target = Folder;
        VoiceWriter.submit("clear voice clips", () -> VoiceFiles.deleteTree(Target));
        return Count;
    }

    private void saveIndex() {
        if (Clips.isEmpty()) {
            Path Target = Folder;
            VoiceWriter.submit("remove an empty voice folder", () -> VoiceFiles.deleteTree(Target));
            return;
        }
        List<VoiceClip> Snapshot = sorted();
        VoiceWriter.submit("save the voice index", () -> VoiceFiles.writeIndex(Folder, Snapshot));
    }

    @Nullable
    public List<byte[]> frames(UUID Id) {
        List<byte[]> Known = Cache.get(Id);
        if (Known != null) {
            return Known;
        }
        if (!Clips.containsKey(Id)) {
            return null;
        }
        try {
            List<byte[]> Read = VoiceFiles.readClip(VoiceFiles.clipPath(Folder, Id), Anomaly).Frames();
            Cache.put(Id, Read);
            return Read;
        } catch (IOException Problem) {
            drop(List.of(Id), true);
            return null;
        }
    }

    public boolean holdsSpeaker(UUID Speaker) {
        for (VoiceClip Each : Clips.values()) {
            if (Each.Speaker().equals(Speaker)) {
                return true;
            }
        }
        return false;
    }
}
