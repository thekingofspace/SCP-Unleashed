package net.rai.scp_unleashed.scp002.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp002.MeteorEntity;

import java.lang.ref.WeakReference;

public class MeteorSoundLayer extends AbstractTickableSoundInstance {
    private final WeakReference<MeteorEntity> Meteor;
    private final float Gain;
    private final float PitchLow;
    private final float PitchHigh;
    private final boolean Loop;
    private int Age;
    private int Release;

    public MeteorSoundLayer(MeteorEntity Meteor, SoundEvent Event, float Gain, float PitchLow, float PitchHigh, boolean Loop) {
        super(Event, SoundSource.BLOCKS, RandomSource.create(Meteor.getSeed() * 31L + Event.getLocation().hashCode()));
        this.Meteor = new WeakReference<>(Meteor);
        this.Gain = Gain;
        this.PitchLow = PitchLow;
        this.PitchHigh = PitchHigh;
        this.Loop = Loop;
        this.looping = Loop;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        follow(Meteor, 0.0f);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !isStopped();
    }

    private void follow(MeteorEntity Current, float Shrink) {
        Vec3 Head = Current.getHead(0.0f);
        x = Head.x;
        y = Head.y;
        z = Head.z;
        float Progress = Current.getProgress(0.0f);
        float Rise = Progress * Progress;
        volume = Math.max(0.0f, Gain * (1.0f - Shrink));
        pitch = Mth.lerp(Rise, PitchLow, PitchHigh);
    }

    @Override
    public void tick() {
        MeteorEntity Current = Meteor.get();
        if (Current == null || Current.isRemoved() || Current.level() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        Age++;
        if (Loop && (Current.getPhase() != MeteorEntity.PhaseFlight || Current.getProgress(0.0f) >= 1.0f)) {
            Release++;
            if (Release > 4) {
                stop();
                return;
            }
        }
        follow(Current, Release / 5.0f);
    }
}
