package net.rai.scp_unleashed.flamethrower.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.function.Predicate;

public class FlamerLoopSound extends AbstractTickableSoundInstance {
    private final Entity Source;
    private final Predicate<Entity> Wanted;
    private final float Loudness;
    private final float FadeIn;
    private final float FadeOut;
    private float Fade;

    public FlamerLoopSound(SoundEvent Sound, Entity Source, float Loudness, float Pitch, float FadeIn, float FadeOut, Predicate<Entity> Wanted) {
        super(Sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.Source = Source;
        this.Wanted = Wanted;
        this.Loudness = Loudness;
        this.FadeIn = FadeIn;
        this.FadeOut = FadeOut;
        this.looping = true;
        this.delay = 0;
        this.pitch = Pitch;
        this.Fade = 0.0F;
        this.volume = 0.0F;
        follow();
    }

    private void follow() {
        this.x = Source.getX();
        this.y = Source.getEyeY() - 0.3;
        this.z = Source.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (Source.isRemoved()) {
            stop();
            return;
        }
        follow();
        boolean Keep = Wanted.test(Source);
        Fade = Keep ? Math.min(1.0F, Fade + FadeIn) : Math.max(0.0F, Fade - FadeOut);
        volume = Loudness * Fade;
        if (!Keep && Fade <= 0.0F) {
            stop();
        }
    }

    public boolean finished() {
        return isStopped();
    }
}
