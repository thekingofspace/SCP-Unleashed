package net.rai.scp_unleashed.warhead.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class WarheadSirenSound extends AbstractTickableSoundInstance {
    public WarheadSirenSound(SoundEvent Event, float Pitch, float Volume) {
        super(Event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.looping = false;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.pitch = Pitch;
        this.volume = Volume;
    }

    public void drive(float Pitch, float Volume) {
        this.pitch = Pitch;
        this.volume = Math.max(0.0f, Volume);
    }

    public void finish() {
        stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
    }
}
