package net.rai.scp_unleashed.scp151.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.rai.scp_unleashed.scp151.Scp151Registry;

public class Scp151AmbienceSound extends AbstractTickableSoundInstance {
    public static final float Loudness = 0.55f;
    public static final float Silent = 0.01f;

    public Scp151AmbienceSound() {
        super(Scp151Registry.GazeLoop.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = Silent;
        this.pitch = 1.0f;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        float Strength = Scp151Overlay.intensity();
        volume = Math.max(Silent, Loudness * Strength);
        if (Strength <= 0.0f && !Scp151ClientState.isGazing()) {
            stop();
        }
    }
}
