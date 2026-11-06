package net.rai.scp_unleashed.voice;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VoiceRecorder {
    public static final int MinFrames = 50;
    public static final int MaxFrames = 150;
    public static final long GapMillis = 300L;
    public static final int PendingCap = 64;
    public static final int MaxFrameBytes = 1500;

    private final Map<Key, Pending> Open = new HashMap<>();
    private final List<Finished> Done = new ArrayList<>();

    public record Finished(String Anomaly, UUID Speaker, String SpeakerName, @Nullable UUID Listener, long Recorded, List<byte[]> Frames) {
        public Finished {
            Frames = List.copyOf(Frames);
        }
    }

    private record Key(String Anomaly, UUID Speaker) {
    }

    private static final class Pending {
        private final List<byte[]> Frames = new ArrayList<>();
        private final String Name;
        @Nullable
        private final UUID Listener;
        private final long Started;
        private long Last;

        private Pending(String Name, @Nullable UUID Listener, long Started) {
            this.Name = Name;
            this.Listener = Listener;
            this.Started = Started;
        }
    }

    public synchronized boolean offer(String Anomaly, UUID Speaker, String Name, @Nullable UUID Listener, byte[] Frame, long NowMillis) {
        if (Frame == null || Frame.length == 0 || Frame.length > MaxFrameBytes) {
            return false;
        }
        Key Which = new Key(Anomaly, Speaker);
        Pending Current = Open.get(Which);
        if (Current != null && NowMillis - Current.Last > GapMillis) {
            finish(Which, Current);
            Current = null;
        }
        if (Current == null) {
            if (Open.size() >= PendingCap) {
                return false;
            }
            Current = new Pending(Name, Listener, NowMillis);
            Open.put(Which, Current);
        }
        Current.Frames.add(Arrays.copyOf(Frame, Frame.length));
        Current.Last = NowMillis;
        if (Current.Frames.size() >= MaxFrames) {
            finish(Which, Current);
        }
        return true;
    }

    public synchronized void sweep(long NowMillis) {
        List<Map.Entry<Key, Pending>> Stale = new ArrayList<>();
        for (Map.Entry<Key, Pending> Entry : Open.entrySet()) {
            if (NowMillis - Entry.getValue().Last > GapMillis) {
                Stale.add(Entry);
            }
        }
        for (Map.Entry<Key, Pending> Entry : Stale) {
            finish(Entry.getKey(), Entry.getValue());
        }
    }

    private void finish(Key Which, Pending Current) {
        Open.remove(Which);
        if (Current.Frames.size() >= MinFrames) {
            Done.add(new Finished(Which.Anomaly(), Which.Speaker(), Current.Name, Current.Listener, Current.Started, Current.Frames));
        }
    }

    public synchronized List<Finished> drain() {
        List<Finished> Taken = new ArrayList<>(Done);
        Done.clear();
        return Taken;
    }

    public synchronized int pending() {
        return Open.size();
    }

    public synchronized void forgetSpeaker(UUID Speaker) {
        Open.keySet().removeIf(Which -> Which.Speaker().equals(Speaker));
        Done.removeIf(Each -> Each.Speaker().equals(Speaker));
    }

    public synchronized void forgetAnomaly(String Anomaly) {
        Open.keySet().removeIf(Which -> Which.Anomaly().equals(Anomaly));
        Done.removeIf(Each -> Each.Anomaly().equals(Anomaly));
    }

    public synchronized void clear() {
        Open.clear();
        Done.clear();
    }
}
