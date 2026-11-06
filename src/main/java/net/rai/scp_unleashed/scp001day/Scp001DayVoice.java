package net.rai.scp_unleashed.scp001day;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.voice.VoiceArchive;
import net.rai.scp_unleashed.voice.VoicePolicy;

public final class Scp001DayVoice {
    public static final int CaptureTicks = 40;
    public static final int CaptureEvery = 20;
    public static final int ReplayMinTicks = 300;
    public static final int ReplayMaxTicks = 800;
    public static final double ReplayRange = 24.0;

    private Scp001DayVoice() {
    }

    public static boolean capture(ServerPlayer Speaker) {
        return VoiceArchive.capture(VoicePolicy.Scp001Day, Speaker, CaptureTicks);
    }

    public static boolean capturing(UUID Speaker) {
        return VoiceArchive.isCapturing(VoicePolicy.Scp001Day, Speaker);
    }

    public static void stop(UUID Speaker) {
        VoiceArchive.stopCapture(VoicePolicy.Scp001Day, Speaker);
    }

    public static boolean replay(Entity Source, UUID Speaker, @Nullable UUID Avoid) {
        return VoiceArchive.replay(VoicePolicy.Scp001Day, Source, Optional.of(Speaker), Optional.ofNullable(Avoid), 1.0f);
    }

    public static int nextReplay(RandomSource Random) {
        return ReplayMinTicks + Random.nextInt(ReplayMaxTicks - ReplayMinTicks + 1);
    }
}
