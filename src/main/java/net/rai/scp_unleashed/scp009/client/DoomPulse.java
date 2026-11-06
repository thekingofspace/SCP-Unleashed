package net.rai.scp_unleashed.scp009.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.rai.scp_unleashed.scp009.Scp009Doom;
import net.rai.scp_unleashed.scp009.Scp009Registry;

public final class DoomPulse {
    public static final int SlowBeat = 30;
    public static final int FastBeat = 10;
    public static final int BeatLength = 3;
    public static final float QuietVolume = 0.45f;
    public static final float LoudVolume = 0.95f;
    private static int Countdown;
    private static int SinceBeat = 100;

    private DoomPulse() {
    }

    public static void tick(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        MobEffectInstance Instance = Scp009Doom.instance(User);
        if (Instance == null) {
            reset();
            return;
        }
        float Progress = Scp009Doom.progress(Instance);
        SinceBeat++;
        Countdown--;
        if (Countdown <= 0) {
            Countdown = Math.round(Mth.lerp(Progress, SlowBeat, FastBeat));
            SinceBeat = 0;
            float Volume = Mth.lerp(Progress, QuietVolume, LoudVolume);
            Client.getSoundManager().play(SimpleSoundInstance.forUI(Scp009Registry.Heartbeat.get(), 1.0f, Volume));
        }
    }

    public static boolean isBeating() {
        return SinceBeat < BeatLength;
    }

    public static void reset() {
        Countdown = 0;
        SinceBeat = 100;
    }
}
