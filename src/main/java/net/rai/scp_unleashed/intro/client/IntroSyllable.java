package net.rai.scp_unleashed.intro.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class IntroSyllable extends AbstractTickableSoundInstance {
    public static final float[] Release = {0.55f, 0.22f};
    public static final float Onset = 0.45f;
    private final float Peak;
    private final int Hold;
    private int Age;

    public IntroSyllable(SoundEvent Sound, RandomSource Picker, float Peak, float Pitch, int Hold) {
        super(Sound, SoundSource.VOICE, Picker);
        this.Peak = Peak;
        this.Hold = Math.max(1, Hold);
        this.volume = Peak * Onset;
        this.pitch = Pitch;
        this.looping = false;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
    }

    @Override
    public void tick() {
        Age++;
        if (Age <= Hold) {
            volume = Peak;
            return;
        }
        int Fade = Age - Hold - 1;
        if (Fade < Release.length) {
            volume = Peak * Release[Fade];
            return;
        }
        stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
