package net.rai.scp_unleashed.liquidsound;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public record LiquidSoundSet(List<DeferredHolder<SoundEvent, SoundEvent>> Swim, List<DeferredHolder<SoundEvent, SoundEvent>> Splash,
                             List<DeferredHolder<SoundEvent, SoundEvent>> HeavySplash, List<DeferredHolder<SoundEvent, SoundEvent>> Flow,
                             List<DeferredHolder<SoundEvent, SoundEvent>> Bubble) {
}
