package net.rai.scp_unleashed.scp2845.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp2845.Scp2845Comet;

import java.lang.ref.WeakReference;

public class Scp2845CometSound extends AbstractTickableSoundInstance {
    private final WeakReference<Scp2845Comet> Comet;
    private final float Gain;
    private final float PitchLow;
    private final float PitchHigh;
    private int Release;

    public Scp2845CometSound(Scp2845Comet Comet, SoundEvent Event, float Gain, float PitchLow, float PitchHigh) {
        super(Event, SoundSource.BLOCKS, RandomSource.create(Comet.getSeed() * 31L + Event.getLocation().hashCode()));
        this.Comet = new WeakReference<>(Comet);
        this.Gain = Gain;
        this.PitchLow = PitchLow;
        this.PitchHigh = PitchHigh;
        this.looping = true;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        follow(Comet, 0.0f);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    private void follow(Scp2845Comet Current, float Shrink) {
        Vec3 Head = Current.getHead(0.0f);
        x = Head.x;
        y = Head.y;
        z = Head.z;
        float Progress = Current.getProgress(0.0f);
        volume = Math.max(0.0f, Gain * (1.0f - Shrink));
        pitch = Mth.lerp(Progress * Progress, PitchLow, PitchHigh);
    }

    @Override
    public void tick() {
        Scp2845Comet Current = Comet.get();
        if (Current == null || Current.isRemoved() || Current.level() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        if (Current.getPhase() != Scp2845Comet.PhaseFlight || Current.getProgress(0.0f) >= 1.0f) {
            Release++;
            if (Release > 4) {
                stop();
                return;
            }
        }
        follow(Current, Release / 5.0f);
    }
}
