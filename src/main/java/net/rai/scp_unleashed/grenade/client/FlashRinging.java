package net.rai.scp_unleashed.grenade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.rai.scp_unleashed.grenade.GrenadeRegistry;
import net.rai.scp_unleashed.grenade.GrenadeTuning;

public final class FlashRinging {
    public static final float PingGain = 0.85F;
    public static final float ChimeGain = 0.55F;
    public static final float SparkleGain = 1.0F;
    public static final float WhineGain = 1.0F;

    private static int Countdown;

    private FlashRinging() {
    }

    public static void start() {
        play(GrenadeRegistry.FlashPing.get(), PingGain);
        play(GrenadeRegistry.FlashChime.get(), ChimeGain);
        play(GrenadeRegistry.FlashSparkle.get(), SparkleGain);
        play(GrenadeRegistry.FlashTinnitus.get(), WhineGain);
        Countdown = GrenadeTuning.RingGrainTicks;
    }

    public static void tick(float Level) {
        if (Level <= GrenadeTuning.RingFloor) {
            Countdown = 0;
            return;
        }
        if (--Countdown <= 0) {
            play(GrenadeRegistry.FlashTinnitus.get(), WhineGain);
            Countdown = GrenadeTuning.RingGrainTicks;
        }
    }

    public static void reset() {
        Countdown = 0;
    }

    private static void play(SoundEvent Sound, float Gain) {
        if (FlashOverlay.ring() > GrenadeTuning.RingFloor) {
            Minecraft.getInstance().getSoundManager().play(new FlashRingSound(Sound, Gain));
        }
    }
}
