package net.rai.scp_unleashed.speaker;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.wire.WireChannels;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SoundMessages {
    public static final String Type = "sound";
    public static final String SoundKey = "Sound";
    public static final String VolumeKey = "Volume";
    public static final String PitchKey = "Pitch";
    public static final String LoopKey = "Loop";
    public static final String ActionKey = "Action";
    public static final String PlayAction = "play";
    public static final String StopAction = "stop";
    public static final float DefaultVolume = 1.0f;
    public static final float DefaultPitch = 1.0f;
    public static final boolean DefaultLoop = false;
    public static final float MinVolume = 0.0f;
    public static final float MaxVolume = 4.0f;
    public static final float MinPitch = 0.5f;
    public static final float MaxPitch = 2.0f;
    public static final SoundSource Source = SoundSource.BLOCKS;
    public static final List<String> FormatGuide = List.of(
            "Channel: sound/<port>, port 0-9999.",
            "Play once: {Sound: \"namespace:path\", Volume: 0.0-4.0 (default 1.0), Pitch: 0.5-2.0 (default 1.0), Loop: false}.",
            "Loop: the same payload with Loop: true. Every speaker on the port keeps the sound looping until it is told otherwise.",
            "Stop: {Action: \"stop\"}. Every speaker on the port ends its loop.",
            "Action is optional and defaults to \"play\". A payload without Loop plays once, so older senders keep working.",
            "A speaker ends its loop when it gets a stop, plays a one-shot, starts another loop, changes port or is broken.");

    private SoundMessages() {
    }

    public static String channel(int Port) {
        return WireChannels.onPort(Type, Port);
    }

    public static float clampVolume(float Volume) {
        return Float.isFinite(Volume) ? Mth.clamp(Volume, MinVolume, MaxVolume) : DefaultVolume;
    }

    public static float clampPitch(float Pitch) {
        return Float.isFinite(Pitch) ? Mth.clamp(Pitch, MinPitch, MaxPitch) : DefaultPitch;
    }

    public static CompoundTag payload(ResourceLocation Sound, float Volume, float Pitch) {
        return payload(Sound, Volume, Pitch, DefaultLoop);
    }

    public static CompoundTag payload(ResourceLocation Sound, float Volume, float Pitch, boolean Loop) {
        CompoundTag Payload = new CompoundTag();
        Payload.putString(SoundKey, Sound.toString());
        Payload.putFloat(VolumeKey, clampVolume(Volume));
        Payload.putFloat(PitchKey, clampPitch(Pitch));
        Payload.putBoolean(LoopKey, Loop);
        return Payload;
    }

    public static CompoundTag stopPayload() {
        CompoundTag Payload = new CompoundTag();
        Payload.putString(ActionKey, StopAction);
        return Payload;
    }

    public static String actionOf(CompoundTag Payload) {
        return Payload.contains(ActionKey, Tag.TAG_STRING) ? Payload.getString(ActionKey) : PlayAction;
    }

    public static boolean isStop(CompoundTag Payload) {
        return StopAction.equals(actionOf(Payload));
    }

    @Nullable
    public static Request read(CompoundTag Payload) {
        if (!PlayAction.equals(actionOf(Payload)) || !Payload.contains(SoundKey, Tag.TAG_STRING)) {
            return null;
        }
        ResourceLocation Id = ResourceLocation.tryParse(Payload.getString(SoundKey));
        if (Id == null) {
            return null;
        }
        float Volume = Payload.contains(VolumeKey, Tag.TAG_ANY_NUMERIC) ? clampVolume(Payload.getFloat(VolumeKey)) : DefaultVolume;
        float Pitch = Payload.contains(PitchKey, Tag.TAG_ANY_NUMERIC) ? clampPitch(Payload.getFloat(PitchKey)) : DefaultPitch;
        boolean Loop = Payload.contains(LoopKey, Tag.TAG_ANY_NUMERIC) ? Payload.getBoolean(LoopKey) : DefaultLoop;
        return new Request(Id, Volume, Pitch, Loop);
    }

    public static Holder<SoundEvent> soundOf(ResourceLocation Id) {
        return BuiltInRegistries.SOUND_EVENT.getHolder(Id).<Holder<SoundEvent>>map(Found -> Found)
                .orElseGet(() -> Holder.direct(SoundEvent.createVariableRangeEvent(Id)));
    }

    public static void play(ServerLevel World, Vec3 At, Request Sound) {
        World.playSound(null, At.x, At.y, At.z, soundOf(Sound.Id()), Source, Sound.Volume(), Sound.Pitch());
    }

    public record Request(ResourceLocation Id, float Volume, float Pitch, boolean Loop) {
        public CompoundTag toPayload() {
            return payload(Id, Volume, Pitch, Loop);
        }

        public boolean sameSound(@Nullable Request Other) {
            return Other != null && Id.equals(Other.Id()) && Volume == Other.Volume() && Pitch == Other.Pitch();
        }
    }
}
