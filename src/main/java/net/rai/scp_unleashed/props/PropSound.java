package net.rai.scp_unleashed.props;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public record PropSound(DeferredHolder<SoundEvent, SoundEvent> Event, int Delay) {
    public static PropSound now(DeferredHolder<SoundEvent, SoundEvent> Event) {
        return new PropSound(Event, 0);
    }

    public static PropSound after(DeferredHolder<SoundEvent, SoundEvent> Event, int Delay) {
        return new PropSound(Event, Delay);
    }
}
