package net.rai.scp_unleashed.scp354.beasts;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public record Scp354Voice(List<DeferredHolder<SoundEvent, SoundEvent>> Idle, List<DeferredHolder<SoundEvent, SoundEvent>> Hurt,
                          List<DeferredHolder<SoundEvent, SoundEvent>> Death, List<DeferredHolder<SoundEvent, SoundEvent>> Step,
                          List<DeferredHolder<SoundEvent, SoundEvent>> Emerge, float Pitch, float StepVolume) {
}
