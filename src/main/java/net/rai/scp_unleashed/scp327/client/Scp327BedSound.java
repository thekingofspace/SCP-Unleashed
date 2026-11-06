package net.rai.scp_unleashed.scp327.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp327.Scp327Siren;

import java.util.function.Supplier;

public class Scp327BedSound extends AbstractTickableSoundInstance {
    private final Scp327Siren Creature;
    private final float Base;
    private final Supplier<Float> Fade;

    public Scp327BedSound(Scp327Siren Creature, SoundEvent Sound, float Base, float Pitch, Supplier<Float> Fade) {
        super(Sound, Scp327Choir.Channel, SoundInstance.createUnseededRandom());
        this.Creature = Creature;
        this.Base = Base;
        this.Fade = Fade;
        this.pitch = Pitch;
        this.volume = Base * Fade.get();
        this.looping = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        Vec3 Mouth = Creature.songPoint();
        this.x = Mouth.x;
        this.y = Mouth.y;
        this.z = Mouth.z;
    }

    @Override
    public void tick() {
        if (Creature.isRemoved()) {
            stop();
            return;
        }
        Vec3 Mouth = Creature.songPoint();
        x = Mouth.x;
        y = Mouth.y;
        z = Mouth.z;
        volume = Math.max(0.0001f, Base * Fade.get());
    }

    public void finish() {
        stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
