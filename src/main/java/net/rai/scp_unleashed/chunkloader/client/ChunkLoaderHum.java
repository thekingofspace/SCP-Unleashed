package net.rai.scp_unleashed.chunkloader.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderBlockEntity;

import java.lang.ref.WeakReference;

public class ChunkLoaderHum extends AbstractTickableSoundInstance {
    public static final float Attack = 0.12f;
    public static final float Release = 0.05f;

    private final WeakReference<ChunkLoaderBlockEntity> Owner;
    private final float Loudness;
    private final float BasePitch;
    private float Smoothed;

    public ChunkLoaderHum(ChunkLoaderBlockEntity Owner, SoundEvent Cue, float Loudness, float BasePitch) {
        super(Cue, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Owner = new WeakReference<>(Owner);
        this.Loudness = Loudness;
        this.BasePitch = BasePitch;
        Vec3 Where = Vec3.atCenterOf(Owner.getBlockPos());
        this.x = Where.x;
        this.y = Where.y;
        this.z = Where.z;
        this.looping = false;
        this.delay = 0;
        this.volume = 0.0f;
        this.pitch = BasePitch;
    }

    public static float strength(ChunkLoaderBlockEntity Loader) {
        return switch (Loader.getStatus()) {
            case Active -> 1.0f;
            case Starting -> 0.8f;
            case LowPower -> 0.55f;
            default -> 0.0f;
        };
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        ChunkLoaderBlockEntity Current = Owner.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Wanted = strength(Current);
        Smoothed += (Wanted - Smoothed) * (Wanted > Smoothed ? Attack : Release);
        volume = Math.max(0.0f, Smoothed * Loudness);
        pitch = BasePitch * (0.85f + 0.15f * Smoothed);
        if (Wanted <= 0.0f && Smoothed < 0.01f) {
            stop();
        }
    }
}
