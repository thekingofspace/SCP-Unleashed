package net.rai.scp_unleashed.storage;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public record StorageCue(DeferredHolder<SoundEvent, SoundEvent> Event, int Delay) {
}
