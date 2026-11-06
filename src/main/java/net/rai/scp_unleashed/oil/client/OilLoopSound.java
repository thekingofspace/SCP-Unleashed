package net.rai.scp_unleashed.oil.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.oil.OilMachineBlockEntity;

import java.lang.ref.WeakReference;

public class OilLoopSound extends AbstractTickableSoundInstance {
    public static final float Silent = 0.015f;

    private final WeakReference<OilMachineBlockEntity> Machine;
    private final float Loudness;
    private final float LowPitch;
    private final float PitchRange;

    public OilLoopSound(OilMachineBlockEntity Machine, SoundEvent Loop, float Loudness, float LowPitch, float PitchRange) {
        super(Loop, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Machine = new WeakReference<>(Machine);
        this.Loudness = Loudness;
        this.LowPitch = LowPitch;
        this.PitchRange = PitchRange;
        Vec3 Centre = Machine.getCentre();
        this.looping = true;
        this.delay = 0;
        this.volume = volumeFor(Machine.getChurn());
        this.pitch = LowPitch + PitchRange * Machine.getChurn();
        this.x = Centre.x;
        this.y = Centre.y;
        this.z = Centre.z;
    }

    private float volumeFor(float Churn) {
        return Loudness * Math.min(1.0f, Churn * 1.6f);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        OilMachineBlockEntity Current = Machine.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Churn = Current.getChurn();
        volume = volumeFor(Churn);
        pitch = LowPitch + PitchRange * Churn;
        if (Churn < Silent) {
            stop();
        }
    }
}
