package net.rai.scp_unleashed.grenade.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.grenade.GrenadeTuning;

public class FlashRingSound extends AbstractTickableSoundInstance {
    private final float Gain;

    public FlashRingSound(SoundEvent Sound, float Gain) {
        super(Sound, SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.Gain = Gain;
        this.looping = false;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.volume = level();
        this.pitch = 1.0F;
    }

    private float level() {
        return Mth.clamp(Gain * FlashOverlay.ring(), 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        if (FlashOverlay.ring() <= GrenadeTuning.RingFloor) {
            stop();
            return;
        }
        volume = level();
    }
}
