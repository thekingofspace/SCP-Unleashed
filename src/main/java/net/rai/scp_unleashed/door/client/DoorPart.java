package net.rai.scp_unleashed.door.client;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public record DoorPart(DeferredHolder<SoundEvent, SoundEvent> Event, float Loudness, float Pitch, int Delay, int Hold, float Floor, float Lift) {
    public static DoorPart hit(DeferredHolder<SoundEvent, SoundEvent> Event, float Loudness, float Pitch, int Delay, float Floor) {
        return new DoorPart(Event, Loudness, Pitch, Delay, -1, Floor, 0.0f);
    }

    public static DoorPart loop(DeferredHolder<SoundEvent, SoundEvent> Event, float Loudness, float Pitch, float Floor, float Lift) {
        return new DoorPart(Event, Loudness, Pitch, 0, 0, Floor, Lift);
    }

    public static DoorPart grain(DeferredHolder<SoundEvent, SoundEvent> Event, float Loudness, float Pitch, int Hold, float Floor, float Lift) {
        return new DoorPart(Event, Loudness, Pitch, 0, Hold, Floor, Lift);
    }

    public boolean isHit() {
        return Hold < 0;
    }

    public boolean isLoop() {
        return Hold == 0;
    }

    public float loudnessAt(float Speed) {
        return Loudness * (Floor + (1.0f - Floor) * Speed);
    }

    public float pitchAt(float Speed) {
        return Pitch + Lift * Speed;
    }
}
