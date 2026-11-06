package net.rai.scp_unleashed.fission.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.function.ToDoubleFunction;

public class FissionLoopSound<T extends BlockEntity> extends AbstractTickableSoundInstance {
    private final WeakReference<T> Owner;
    private final ToDoubleFunction<T> Strength;
    private final ToDoubleFunction<T> Tune;
    private final float Loudness;
    private final float Attack;
    private final float Release;
    private float Smoothed;

    public FissionLoopSound(T Owner, SoundEvent Sound, Vec3 Where, float Loudness, float Attack, float Release, boolean Loop, ToDoubleFunction<T> Strength, ToDoubleFunction<T> Tune) {
        super(Sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Owner = new WeakReference<>(Owner);
        this.Strength = Strength;
        this.Tune = Tune;
        this.Loudness = Loudness;
        this.Attack = Attack;
        this.Release = Release;
        this.looping = Loop;
        this.delay = 0;
        this.volume = 0.0f;
        this.pitch = (float) Tune.applyAsDouble(Owner);
        this.x = Where.x;
        this.y = Where.y;
        this.z = Where.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        T Current = Owner.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Wanted = (float) Strength.applyAsDouble(Current);
        Smoothed += (Wanted - Smoothed) * (Wanted > Smoothed ? Attack : Release);
        volume = Math.max(0.0f, Smoothed * Loudness);
        pitch = (float) Tune.applyAsDouble(Current);
        if (Wanted <= 0.0f && Smoothed < 0.01f) {
            stop();
        }
    }
}
