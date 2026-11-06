package net.rai.scp_unleashed.scp018.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp018.BallTuning;
import net.rai.scp_unleashed.scp018.Scp018Ball;
import net.rai.scp_unleashed.scp018.Scp018Registry;

public class BallWhooshSound extends AbstractTickableSoundInstance {
    public static final float MaxVolume = 0.85f;
    public static final float FadeStep = 0.12f;
    public static final int QuietLimit = 30;
    private final Scp018Ball Ball;
    private int QuietTicks;

    public BallWhooshSound(Scp018Ball Ball) {
        super(Scp018Registry.Whoosh.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.Ball = Ball;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.05f;
        this.pitch = 0.8f;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        follow();
    }

    private void follow() {
        Vec3 Centre = Ball.centre();
        this.x = Centre.x;
        this.y = Centre.y;
        this.z = Centre.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (Ball.isRemoved()) {
            volume = Math.max(0.0f, volume - FadeStep * 2.0f);
            if (volume <= 0.0f) {
                stop();
            }
            return;
        }
        follow();
        double Speed = Ball.getDeltaMovement().length();
        float Strength = (float) Mth.clamp((Speed - BallTuning.WhooshSpeed * 0.85) / (BallTuning.ExplodeSpeed - BallTuning.WhooshSpeed * 0.85), 0.0, 1.0);
        float Target = Strength * MaxVolume;
        volume += Mth.clamp(Target - volume, -FadeStep, FadeStep);
        pitch = 0.75f + Strength * 0.85f;
        QuietTicks = Strength <= 0.0f ? QuietTicks + 1 : 0;
        if (QuietTicks > QuietLimit && volume <= 0.01f) {
            stop();
        }
    }

    public boolean finished() {
        return isStopped();
    }
}
