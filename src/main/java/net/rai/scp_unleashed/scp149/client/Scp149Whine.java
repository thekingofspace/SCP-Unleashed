package net.rai.scp_unleashed.scp149.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp149.Scp149Fly;

public class Scp149Whine extends AbstractTickableSoundInstance {
    public static final float HoverVolume = 0.55f;
    public static final float DartVolume = 0.9f;
    public static final float Fade = 0.15f;
    private final Scp149Fly Fly;
    private final float Weight;
    private final float Tilt;

    public Scp149Whine(Scp149Fly Fly, SoundEvent Sound, float Weight, float Tilt) {
        super(Sound, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.Fly = Fly;
        this.Weight = Weight;
        this.Tilt = Tilt;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.x = Fly.getX();
        this.y = Fly.getY();
        this.z = Fly.getZ();
    }

    @Override
    public boolean canPlaySound() {
        return !Fly.isSilent();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (Fly.isRemoved() || !Fly.isAlive()) {
            stop();
            return;
        }
        x = Fly.getX();
        y = Fly.getY() + Fly.getBbHeight() * 0.5;
        z = Fly.getZ();
        float Goal = Fly.isResting() ? 0.0f : (Fly.isDarting() ? DartVolume : HoverVolume) * Weight;
        volume = Mth.lerp(Fade, volume, Goal);
        pitch = Fly.whinePitch() * Tilt;
    }
}
