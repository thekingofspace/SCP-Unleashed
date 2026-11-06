package net.rai.scp_unleashed.rifttech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class RiftTechLoopSound<T extends BlockEntity> extends AbstractTickableSoundInstance {
    public static final float Silent = 0.008f;

    private final WeakReference<T> Owner;
    private final ToDoubleFunction<T> Loudness;
    private final ToDoubleFunction<T> Tune;
    private final Function<T, Vec3> Place;
    private float Smoothed;

    public RiftTechLoopSound(T Owner, SoundEvent Sound, ToDoubleFunction<T> Loudness, ToDoubleFunction<T> Tune, Function<T, Vec3> Place) {
        super(Sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Owner = new WeakReference<>(Owner);
        this.Loudness = Loudness;
        this.Tune = Tune;
        this.Place = Place;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001f;
        this.pitch = (float) Tune.applyAsDouble(Owner);
        follow(Owner);
    }

    private void follow(T Current) {
        Vec3 Spot = Place.apply(Current);
        x = Spot.x;
        y = Spot.y;
        z = Spot.z;
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
        float Wanted = (float) Loudness.applyAsDouble(Current);
        Smoothed += (Wanted - Smoothed) * (Wanted > Smoothed ? 0.1f : 0.06f);
        volume = Math.max(Smoothed, 0.001f);
        pitch = (float) Tune.applyAsDouble(Current);
        follow(Current);
        if (Wanted <= 0.0f && Smoothed < Silent) {
            stop();
        }
    }
}
