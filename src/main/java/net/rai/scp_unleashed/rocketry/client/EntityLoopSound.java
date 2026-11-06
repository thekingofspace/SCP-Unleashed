package net.rai.scp_unleashed.rocketry.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.lang.ref.WeakReference;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public class EntityLoopSound extends AbstractTickableSoundInstance {
    public static final float FadeStep = 0.25f;

    private final WeakReference<Entity> Source;
    private final ToDoubleFunction<Entity> Loudness;
    private final ToDoubleFunction<Entity> Tone;
    private final Predicate<Entity> Finished;
    private boolean Ending;

    public EntityLoopSound(Entity Source, SoundEvent Played, boolean Looping, ToDoubleFunction<Entity> Loudness, ToDoubleFunction<Entity> Tone, Predicate<Entity> Finished) {
        super(Played, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Source = new WeakReference<>(Source);
        this.Loudness = Loudness;
        this.Tone = Tone;
        this.Finished = Finished;
        this.looping = Looping;
        this.delay = 0;
        this.volume = (float) Math.max(0.0, Loudness.applyAsDouble(Source));
        this.pitch = (float) Tone.applyAsDouble(Source);
        this.x = Source.getX();
        this.y = Source.getY();
        this.z = Source.getZ();
    }

    public static EntityLoopSound loop(Entity Source, SoundEvent Played, ToDoubleFunction<Entity> Loudness, ToDoubleFunction<Entity> Tone, Predicate<Entity> Finished) {
        return new EntityLoopSound(Source, Played, true, Loudness, Tone, Finished);
    }

    public static EntityLoopSound once(Entity Source, SoundEvent Played, double Loudness, double Tone, Predicate<Entity> Finished) {
        return new EntityLoopSound(Source, Played, false, Each -> Loudness, Each -> Tone, Finished);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        Entity Current = Source.get();
        if (Current == null || Current.isRemoved() || Current.level() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        x = Current.getX();
        y = Current.getY();
        z = Current.getZ();
        if (!Ending && Finished.test(Current)) {
            Ending = true;
        }
        float Wanted = Ending ? 0.0f : (float) Math.max(0.0, Loudness.applyAsDouble(Current));
        volume = Wanted >= volume ? Wanted : Math.max(Wanted, Math.min(volume, 1.0f) - FadeStep);
        pitch = (float) Tone.applyAsDouble(Current);
        if (Ending && volume <= 0.0f) {
            stop();
        }
    }
}
