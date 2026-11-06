package net.rai.scp_unleashed.fusion.client;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.rai.scp_unleashed.fusion.FusionReactorBlockEntity;

import java.util.function.ToDoubleFunction;

final class FusionLoopChorus {
    private static final double TickSeconds = 0.05;
    private final SoundEvent Voice;
    private final double Stagger;
    private final ToDoubleFunction<FusionReactorBlockEntity> Strength;
    private final ToDoubleFunction<FusionReactorBlockEntity> Tune;
    private final FusionLoopSound Lead;
    private final FusionLoopSound Under;
    private FusionLoopSound Follower;
    private double Travel;

    FusionLoopChorus(SoundManager Sounds, FusionReactorBlockEntity Reactor, SoundEvent Voice, SoundEvent Bed, double Stagger,
                     ToDoubleFunction<FusionReactorBlockEntity> Strength, ToDoubleFunction<FusionReactorBlockEntity> Tune) {
        this.Voice = Voice;
        this.Stagger = Stagger;
        this.Strength = Strength;
        this.Tune = Tune;
        Lead = new FusionLoopSound(Reactor, Voice, 1.0f, Strength, Tune);
        Sounds.play(Lead);
        if (Bed != null) {
            Under = new FusionLoopSound(Reactor, Bed, 1.0f, Strength, Tune);
            Sounds.play(Under);
        } else {
            Under = null;
        }
    }

    boolean isPlaying(SoundManager Sounds) {
        return Sounds.isActive(Lead);
    }

    void tick(SoundManager Sounds, FusionReactorBlockEntity Reactor) {
        if (Follower != null) {
            return;
        }
        Travel += Tune.applyAsDouble(Reactor) * TickSeconds;
        if (Travel >= Stagger) {
            Follower = new FusionLoopSound(Reactor, Voice, 1.0f, Strength, Tune);
            Sounds.play(Follower);
        }
    }

    void stop(SoundManager Sounds) {
        Sounds.stop(Lead);
        if (Under != null) {
            Sounds.stop(Under);
        }
        if (Follower != null) {
            Sounds.stop(Follower);
        }
    }
}
