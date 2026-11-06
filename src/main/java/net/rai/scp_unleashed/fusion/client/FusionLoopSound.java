package net.rai.scp_unleashed.fusion.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.fusion.FusionReactorBlockEntity;

import java.lang.ref.WeakReference;
import java.util.function.ToDoubleFunction;

public class FusionLoopSound extends AbstractTickableSoundInstance {
    private final WeakReference<FusionReactorBlockEntity> Reactor;
    private final ToDoubleFunction<FusionReactorBlockEntity> Strength;
    private final ToDoubleFunction<FusionReactorBlockEntity> Tune;
    private final float Loudness;
    private float Smoothed;

    public FusionLoopSound(FusionReactorBlockEntity Reactor, SoundEvent Sound, float Loudness, ToDoubleFunction<FusionReactorBlockEntity> Strength, ToDoubleFunction<FusionReactorBlockEntity> Tune) {
        super(Sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Reactor = new WeakReference<>(Reactor);
        this.Strength = Strength;
        this.Tune = Tune;
        this.Loudness = Loudness;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.pitch = (float) Tune.applyAsDouble(Reactor);
        Vec3 Middle = Reactor.centre(Reactor.getBlockState());
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
        FusionReactorBlockEntity Current = Reactor.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Wanted = (float) Strength.applyAsDouble(Current);
        Smoothed += (Wanted - Smoothed) * (Wanted > Smoothed ? 0.2f : 0.1f);
        volume = Smoothed * Loudness;
        pitch = (float) Tune.applyAsDouble(Current);
        if (Wanted <= 0.0f && Smoothed < 0.01f) {
            stop();
        }
    }
}
