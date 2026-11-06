package net.rai.scp_unleashed.scp163.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp163.Scp163CraftEntity;

import java.lang.ref.WeakReference;

public class Scp163CraftSound extends AbstractTickableSoundInstance {
    private final WeakReference<Scp163CraftEntity> Craft;
    private final float Gain;
    private final float PitchLow;
    private final float PitchHigh;
    private int Release;

    public Scp163CraftSound(Scp163CraftEntity Craft, SoundEvent Event, float Gain, float PitchLow, float PitchHigh) {
        super(Event, SoundSource.BLOCKS, RandomSource.create(Craft.getSeed() * 31L + Event.getLocation().hashCode()));
        this.Craft = new WeakReference<>(Craft);
        this.Gain = Gain;
        this.PitchLow = PitchLow;
        this.PitchHigh = PitchHigh;
        this.looping = true;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        follow(Craft, 0.0f);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !isStopped();
    }

    private void follow(Scp163CraftEntity Current, float Shrink) {
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
        Scp163CraftEntity Current = Craft.get();
        if (Current == null || Current.isRemoved() || Current.level() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        if (Current.getPhase() != Scp163CraftEntity.PhaseFlight || Current.getProgress(0.0f) >= 1.0f) {
            Release++;
            if (Release > 4) {
                stop();
                return;
            }
        }
        follow(Current, Release / 5.0f);
    }
}
