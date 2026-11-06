package net.rai.scp_unleashed.scp5001.xk;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.core.FightAudio;

import static net.rai.scp_unleashed.scp5001.core.FightAudio.layer;

public final class YaldabaothVoice {
    public static final int RoarRest = 70;
    public static final int FlinchRest = 36;
    public static final int PresenceMin = 300;
    public static final int PresenceSpread = 300;
    private static int NextPresence = 200;

    private YaldabaothVoice() {
    }

    public static void reset() {
        NextPresence = 200;
    }

    public static void tick(ServerLevel World, YaldabaothFlight Flight) {
        if (!Flight.present()) {
            return;
        }
        FightAudio.tick(World);
        int Said = Flight.voice();
        float Power = Flight.voicePower();
        Vec3 Head = Flight.head();
        YaldabaothFlight.Mode Mode = Flight.mode();
        if (Said != 0) {
            speak(World, Head, Said, Power);
        }
        if (--NextPresence <= 0) {
            NextPresence = PresenceMin + World.random.nextInt(PresenceSpread);
            boolean Busy = Mode == YaldabaothFlight.Mode.Attack || Mode == YaldabaothFlight.Mode.React || Mode == YaldabaothFlight.Mode.Bind
                    || Mode == YaldabaothFlight.Mode.Banish;
            if (!Busy && FightAudio.ready(World, "xk.roar", 120)) {
                FightAudio.cue(World, Head, "xk.presence", 200, SoundSource.HOSTILE, layer(Scp5001XkRegistry.Ambient), layer(Scp5001XkRegistry.Rumble, 1.0f, 0.8f, 14));
            }
        }
    }

    public static void hurt(ServerLevel World, Vec3 At) {
        FightAudio.cue(World, At, "xk.hurt", 24, SoundSource.HOSTILE, layer(Scp5001XkRegistry.Flinch, 1.0f, 0.6f, 0));
    }

    private static void speak(ServerLevel World, Vec3 Head, int Said, float Power) {
        if ((Said & YaldabaothFlight.VoiceBanish) != 0) {
            FightAudio.cue(World, Head, "xk.banish", 0, SoundSource.HOSTILE, layer(Scp5001XkRegistry.RoarGreat), layer(Scp5001XkRegistry.Vanish, 1.0f, 3),
                    layer(Scp5001XkRegistry.RoarLow, 0.92f, 0.8f, 10));
            return;
        }
        if ((Said & YaldabaothFlight.VoiceDrag) != 0) {
            FightAudio.cue(World, Head, "xk.drag", 120, SoundSource.HOSTILE, layer(Scp5001XkRegistry.RoarGreat), layer(Scp5001XkRegistry.Roar, 0.96f, 4),
                    layer(Scp5001XkRegistry.Coil, 1.0f, 0.8f, 10));
        }
        boolean Flinched = false;
        if ((Said & YaldabaothFlight.VoiceFlinch) != 0) {
            Flinched = FightAudio.cue(World, Head, "xk.flinch", FlinchRest, SoundSource.HOSTILE, layer(Scp5001XkRegistry.Flinch, 1.0f, Math.min(1.0f, 0.55f + Power * 0.3f), 0));
        }
        if ((Said & YaldabaothFlight.VoiceRoar) != 0) {
            int Lead = Flinched ? 8 : 0;
            if ((Said & YaldabaothFlight.VoiceGreat) != 0) {
                FightAudio.cue(World, Head, "xk.roar", RoarRest, SoundSource.HOSTILE, layer(Scp5001XkRegistry.Roar, 1.0f, Lead), layer(Scp5001XkRegistry.RoarLow, 0.95f, Lead + 2),
                        layer(Scp5001XkRegistry.RoarGreat, 1.0f, Lead + 5));
            } else {
                FightAudio.cue(World, Head, "xk.roar", RoarRest, SoundSource.HOSTILE, layer(Scp5001XkRegistry.Roar, 1.0f, Lead), layer(Scp5001XkRegistry.RoarLow, 1.0f, 0.8f, Lead + 3));
            }
        }
        if ((Said & YaldabaothFlight.VoiceDive) != 0) {
            FightAudio.cue(World, Head, "xk.dive", 100, SoundSource.HOSTILE, layer(Scp5001XkRegistry.Lunge), layer(Scp5001XkRegistry.RoarLow, 1.05f, 0.7f, 6));
        }
        if ((Said & YaldabaothFlight.VoiceLunge) != 0) {
            FightAudio.cue(World, Head, "xk.lunge", 20, SoundSource.HOSTILE, layer(Scp5001XkRegistry.Lunge, 1.0f, Power, 0), layer(Scp5001XkRegistry.RoarLow, 1.08f, 0.45f, 4));
        }
        if ((Said & YaldabaothFlight.VoiceSweep) != 0) {
            FightAudio.cue(World, Head, "xk.lunge", 20, SoundSource.HOSTILE, layer(Scp5001XkRegistry.Lunge, 0.94f, 0), layer(Scp5001XkRegistry.Coil, 1.0f, 0.7f, 8));
        }
        if ((Said & YaldabaothFlight.VoiceSnap) != 0) {
            FightAudio.cue(World, Head, "xk.snap", 12, SoundSource.HOSTILE, layer(Scp5001XkRegistry.Bite, 1.0f, Power, 0), layer(Scp5001XkRegistry.Bite, 0.9f, 0.6f * Power, 2));
        }
        if ((Said & YaldabaothFlight.VoiceCoil) != 0) {
            FightAudio.cue(World, Head, "xk.coil", 60, SoundSource.HOSTILE, layer(Scp5001XkRegistry.Coil, 1.0f, Power, 0), layer(Scp5001XkRegistry.RoarLow, 0.92f, 0.5f, 12));
        }
    }
}
