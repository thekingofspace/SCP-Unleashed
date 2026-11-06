package net.rai.scp_unleashed.mining.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ServerLoopSound extends AbstractTickableSoundInstance {
    public static final float Attack = 0.012f;
    public static final float Release = 0.02f;
    public static final float Glide = 0.005f;
    public static final double Follow = 0.12;
    public static final float Silence = 0.002f;

    private Vec3 Target;
    private float TargetVolume;
    private float TargetPitch;
    private boolean Fading;

    public ServerLoopSound(SoundEvent Cue, Vec3 Where) {
        super(Cue, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        Target = Where;
        x = Where.x;
        y = Where.y;
        z = Where.z;
        looping = true;
        delay = 0;
        volume = 0.0f;
        pitch = 1.0f;
        TargetPitch = 1.0f;
    }

    public void aim(Vec3 Where, float Volume, float Pitch) {
        Target = Where;
        TargetVolume = Volume;
        TargetPitch = Pitch;
        Fading = false;
    }

    public void fade() {
        Fading = true;
        TargetVolume = 0.0f;
    }

    public boolean isFading() {
        return Fading;
    }

    public Vec3 target() {
        return Target;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        x += (Target.x - x) * Follow;
        y += (Target.y - y) * Follow;
        z += (Target.z - z) * Follow;
        float Step = TargetVolume > volume ? Attack : Release;
        volume = volume + Mth.clamp(TargetVolume - volume, -Step, Step);
        pitch = Mth.clamp(pitch + Mth.clamp(TargetPitch - pitch, -Glide, Glide), 0.5f, 2.0f);
        if (Fading && volume <= Silence) {
            stop();
        }
    }
}
