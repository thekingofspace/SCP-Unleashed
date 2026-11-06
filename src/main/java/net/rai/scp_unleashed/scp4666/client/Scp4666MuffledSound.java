package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class Scp4666MuffledSound implements TickableSoundInstance {
    public static final int FadeTicks = 40;
    private final SoundInstance Inner;
    private final boolean Fades;
    private int Age;
    private int Leaving = -1;
    private boolean Stopped;

    public Scp4666MuffledSound(SoundInstance Inner, boolean Fades) {
        this.Inner = Inner;
        this.Fades = Fades;
    }

    public void fadeOut() {
        if (Leaving < 0) {
            Leaving = FadeTicks;
        }
    }

    private float fade() {
        if (!Fades) {
            return 1.0f;
        }
        float In = Math.min(1.0f, Age / (float) FadeTicks);
        float Out = Leaving < 0 ? 1.0f : Leaving / (float) FadeTicks;
        return In * In * (3.0f - 2.0f * In) * Out;
    }

    @Override
    public boolean isStopped() {
        return Stopped || Inner instanceof TickableSoundInstance Tickable && Tickable.isStopped();
    }

    @Override
    public void tick() {
        Age++;
        if (Leaving >= 0 && --Leaving <= 0) {
            Stopped = true;
        }
        if (Inner instanceof TickableSoundInstance Tickable) {
            Tickable.tick();
        }
    }

    @Override
    public ResourceLocation getLocation() {
        return Inner.getLocation();
    }

    @Override
    @Nullable
    public WeighedSoundEvents resolve(SoundManager Manager) {
        return Inner.resolve(Manager);
    }

    @Override
    public Sound getSound() {
        return Inner.getSound();
    }

    @Override
    public SoundSource getSource() {
        return Inner.getSource();
    }

    @Override
    public boolean isLooping() {
        return Inner.isLooping();
    }

    @Override
    public boolean isRelative() {
        return Inner.isRelative();
    }

    @Override
    public int getDelay() {
        return Inner.getDelay();
    }

    @Override
    public float getVolume() {
        return Inner.getVolume() * Scp4666Atmosphere.volume() * fade();
    }

    @Override
    public float getPitch() {
        return Inner.getPitch() * Scp4666Atmosphere.pitch();
    }

    @Override
    public double getX() {
        return Inner.getX();
    }

    @Override
    public double getY() {
        return Inner.getY();
    }

    @Override
    public double getZ() {
        return Inner.getZ();
    }

    @Override
    public Attenuation getAttenuation() {
        return Inner.getAttenuation();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return Inner.canPlaySound();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary Buffers, Sound Played, boolean Looping) {
        return Inner.getStream(Buffers, Played, Looping);
    }
}
