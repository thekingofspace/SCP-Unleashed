package net.rai.scp_unleashed.mtf.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.rai.scp_unleashed.mtf.MtfHelicopter;

public class MtfHelicopterSound extends AbstractTickableSoundInstance {
    public static final float[] Chop = {1.0f, 0.62f, 0.34f, 0.62f};
    public static final double Lift = 2.0;
    private final MtfHelicopter Owner;
    private final float Base;
    private final boolean Chopping;

    public MtfHelicopterSound(MtfHelicopter Owner, SoundEvent Event, float Volume, float Pitch, boolean Chopping) {
        super(Event, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.Owner = Owner;
        this.Base = Volume;
        this.Chopping = Chopping;
        looping = true;
        delay = 0;
        volume = Volume;
        pitch = Pitch;
        x = Owner.getX();
        y = Owner.getY() + Lift;
        z = Owner.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (Owner.isRemoved()) {
            stop();
            return;
        }
        x = Owner.getX();
        y = Owner.getY() + Lift;
        z = Owner.getZ();
        volume = Chopping ? Base * Chop[Math.floorMod(Owner.tickCount, Chop.length)] : Base;
    }
}
