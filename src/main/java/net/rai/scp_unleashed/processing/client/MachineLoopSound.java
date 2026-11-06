package net.rai.scp_unleashed.processing.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.rai.scp_unleashed.processing.ProcessingMachineBlockEntity;

import java.lang.ref.WeakReference;

public class MachineLoopSound extends AbstractTickableSoundInstance {
    public static final float FadeIn = 0.2f;
    public static final float FadeOut = 0.08f;
    public static final float Idling = 0.55f;

    private final WeakReference<ProcessingMachineBlockEntity> Machine;
    private final float Loudness;
    private float Smoothed;

    public MachineLoopSound(ProcessingMachineBlockEntity Machine, SoundEvent Sound, float Loudness) {
        super(Sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Machine = new WeakReference<>(Machine);
        this.Loudness = Loudness;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.x = Machine.getBlockPos().getX() + 0.5;
        this.y = Machine.getBlockPos().getY() + 0.5;
        this.z = Machine.getBlockPos().getZ() + 0.5;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        ProcessingMachineBlockEntity Current = Machine.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Wanted = Current.isWorking() ? (Current.getShownRate() > 0.0f ? 1.0f : Idling) : 0.0f;
        Smoothed += (Wanted - Smoothed) * (Wanted > Smoothed ? FadeIn : FadeOut);
        volume = Smoothed * Loudness;
        if (Wanted <= 0.0f && Smoothed < 0.01f) {
            stop();
        }
    }
}
