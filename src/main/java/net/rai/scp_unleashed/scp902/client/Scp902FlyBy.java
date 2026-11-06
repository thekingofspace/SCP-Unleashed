package net.rai.scp_unleashed.scp902.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class Scp902FlyBy extends AbstractTickableSoundInstance {
    private final Vec3 From;
    private final Vec3 To;
    private final int Ticks;
    private int Age;

    public Scp902FlyBy(SoundEvent Sound, Vec3 From, Vec3 To, int Ticks, float Volume, float Pitch, RandomSource Random) {
        super(Sound, SoundSource.AMBIENT, Random);
        this.From = From;
        this.To = To;
        this.Ticks = Math.max(1, Ticks);
        this.volume = Volume;
        this.pitch = Pitch;
        this.looping = false;
        this.delay = 0;
        this.attenuation = Attenuation.LINEAR;
        this.x = From.x;
        this.y = From.y;
        this.z = From.z;
    }

    @Override
    public void tick() {
        Age++;
        double Share = Math.min(1.0, Age / (double) Ticks);
        Vec3 At = From.lerp(To, Share);
        x = At.x;
        y = At.y;
        z = At.z;
        if (Age > Ticks + 10) {
            stop();
        }
    }
}
