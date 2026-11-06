package net.rai.scp_unleashed.voice;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record VoiceClip(UUID Id, String Anomaly, UUID Speaker, String SpeakerName, long Recorded, @Nullable UUID Listener, int Frames, int Bytes) {
    public static final int FrameMillis = 20;

    public int millis() {
        return Frames * FrameMillis;
    }

    public boolean olderThan(long Now, long MaxAge) {
        return Now - Recorded > MaxAge;
    }
}
