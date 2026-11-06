package net.rai.scp_unleashed.refinery.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.refinery.OrePurifierBlockEntity;

import java.lang.ref.WeakReference;

public class PurifierLoopSound extends AbstractTickableSoundInstance {
    public static final float LowPitch = 0.75f;
    public static final float PitchRange = 0.25f;
    public static final float Silent = 0.015f;

    private final WeakReference<OrePurifierBlockEntity> Machine;
    private final float Loudness;

    public PurifierLoopSound(OrePurifierBlockEntity Machine, SoundEvent Loop, float Loudness) {
        super(Loop, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Machine = new WeakReference<>(Machine);
        this.Loudness = Loudness;
        Vec3 Centre = Machine.getCentre();
        this.looping = true;
        this.delay = 0;
        this.volume = volumeFor(Machine.getChurn());
        this.pitch = pitchFor(Machine.getChurn());
        this.x = Centre.x;
        this.y = Centre.y;
        this.z = Centre.z;
    }

    public float volumeFor(float Churn) {
        return Loudness * Math.min(1.0f, Churn * 1.6f);
    }

    public static float pitchFor(float Churn) {
        return LowPitch + PitchRange * Churn;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        OrePurifierBlockEntity Current = Machine.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Churn = Current.getChurn();
        volume = volumeFor(Churn);
        pitch = pitchFor(Churn);
        if (Churn < Silent) {
            stop();
        }
    }
}
