package net.rai.scp_unleashed.scp610;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public record Scp610Voice(List<DeferredHolder<SoundEvent, SoundEvent>> Idle, List<DeferredHolder<SoundEvent, SoundEvent>> Hurt,
                          List<DeferredHolder<SoundEvent, SoundEvent>> Death, List<DeferredHolder<SoundEvent, SoundEvent>> Step,
                          List<DeferredHolder<SoundEvent, SoundEvent>> Attack, float Pitch, float StepVolume) {
    public static Scp610Voice of(DeferredHolder<SoundEvent, SoundEvent> Ambient, DeferredHolder<SoundEvent, SoundEvent> Hurt, DeferredHolder<SoundEvent, SoundEvent> Death,
                                 DeferredHolder<SoundEvent, SoundEvent> Step, DeferredHolder<SoundEvent, SoundEvent> Attack, boolean Heavy, float Pitch, float StepVolume) {
        DeferredHolder<SoundEvent, SoundEvent> Wet = Heavy ? Scp610Sounds.WetHeavy : Scp610Sounds.Wet;
        return new Scp610Voice(List.of(Ambient, Wet), List.of(Hurt, Wet), List.of(Death, Scp610Sounds.WetHeavy), List.of(Step), List.of(Attack, Wet), Pitch, StepVolume);
    }
}
