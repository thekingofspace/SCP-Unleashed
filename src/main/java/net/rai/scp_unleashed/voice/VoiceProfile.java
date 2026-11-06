package net.rai.scp_unleashed.voice;

public interface VoiceProfile {
    short[] apply(short[] Pcm, long Seed, float PitchScale);
}
