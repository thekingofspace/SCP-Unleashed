package net.rai.scp_unleashed.scp1730.rooms.client;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class Scp1730SlowedSound implements SoundInstance {
    public static final float Drag = 0.72f;
    private final SoundInstance Inner;

    public Scp1730SlowedSound(SoundInstance Inner) {
        this.Inner = Inner;
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
        return Inner.getVolume();
    }

    @Override
    public float getPitch() {
        return Inner.getPitch() * Drag;
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
        return Inner.canStartSilent();
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
