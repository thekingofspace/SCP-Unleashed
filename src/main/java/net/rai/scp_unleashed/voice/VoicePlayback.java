package net.rai.scp_unleashed.voice;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.List;

public interface VoicePlayback {
    boolean play(ServerLevel World, Entity Source, List<byte[]> Frames, VoiceProfile Profile, float PitchScale, long Seed);

    default void follow() {
    }

    default void stopAll() {
    }
}
