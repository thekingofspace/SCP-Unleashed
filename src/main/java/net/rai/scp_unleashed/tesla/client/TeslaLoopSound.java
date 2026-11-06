package net.rai.scp_unleashed.tesla.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.tesla.TeslaGateBlockEntity;
import net.rai.scp_unleashed.tesla.TeslaGeometry;

import java.lang.ref.WeakReference;
import java.util.function.ToDoubleFunction;

public class TeslaLoopSound extends AbstractTickableSoundInstance {
    private final WeakReference<TeslaGateBlockEntity> Gate;
    private final ToDoubleFunction<TeslaGateBlockEntity> Strength;
    private final ToDoubleFunction<TeslaGateBlockEntity> Tune;
    private final float Loudness;
    private float Smoothed;

    public TeslaLoopSound(TeslaGateBlockEntity Gate, SoundEvent Sound, float Loudness, ToDoubleFunction<TeslaGateBlockEntity> Strength, ToDoubleFunction<TeslaGateBlockEntity> Tune) {
        super(Sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Gate = new WeakReference<>(Gate);
        this.Strength = Strength;
        this.Tune = Tune;
        this.Loudness = Loudness;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.pitch = (float) Tune.applyAsDouble(Gate);
        Vec3 Middle = TeslaGeometry.toWorld(Gate.getOrigin(), Gate.getFront(), TeslaGeometry.Centre);
        this.x = Middle.x;
        this.y = Middle.y;
        this.z = Middle.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        TeslaGateBlockEntity Current = Gate.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Wanted = (float) Strength.applyAsDouble(Current);
        Smoothed += (Wanted - Smoothed) * (Wanted > Smoothed ? 0.25f : 0.12f);
        volume = Smoothed * Loudness;
        pitch = (float) Tune.applyAsDouble(Current);
        if (Wanted <= 0.0f && Smoothed < 0.01f) {
            stop();
        }
    }
}
