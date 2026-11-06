package net.rai.scp_unleashed.centrifuge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.rai.scp_unleashed.centrifuge.CentrifugeBlockEntity;
import net.rai.scp_unleashed.centrifuge.CentrifugeRegistry;

import java.lang.ref.WeakReference;

public class CentrifugeLoopSound extends AbstractTickableSoundInstance {
    public static final float Loudness = 0.85f;
    public static final float LowPitch = 0.5f;
    public static final float PitchRange = 0.75f;
    public static final float Silent = 0.015f;

    private final WeakReference<CentrifugeBlockEntity> Machine;

    public CentrifugeLoopSound(CentrifugeBlockEntity Machine) {
        super(CentrifugeRegistry.Whir.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Machine = new WeakReference<>(Machine);
        this.looping = true;
        this.delay = 0;
        this.volume = volumeFor(Machine.getSpin());
        this.pitch = pitchFor(Machine.getSpin());
        this.x = Machine.getBlockPos().getX() + 0.5;
        this.y = Machine.getBlockPos().getY() + 0.5;
        this.z = Machine.getBlockPos().getZ() + 0.5;
    }

    public static float volumeFor(float Spin) {
        return Loudness * Math.min(1.0f, Spin * 1.8f);
    }

    public static float pitchFor(float Spin) {
        return LowPitch + PitchRange * Spin;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        CentrifugeBlockEntity Current = Machine.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Spin = Current.getSpin();
        volume = volumeFor(Spin);
        pitch = pitchFor(Spin);
        if (Spin < Silent) {
            stop();
        }
    }
}
