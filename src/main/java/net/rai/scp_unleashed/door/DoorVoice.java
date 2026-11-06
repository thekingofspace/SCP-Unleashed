package net.rai.scp_unleashed.door;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public enum DoorVoice {
    Standard(1.0f, 1.0f, 0.9f, 1.0f),
    Heavy(1.1f, 0.82f, 1.1f, 1.0f),
    Quiet(0.5f, 1.08f, 0.45f, 1.15f),
    Crisp(0.75f, 1.12f, 0.5f, 1.35f),
    Light(0.8f, 1.1f, 0.7f, 1.15f),
    Gate,
    Chamber;

    private final float MotionVolume;
    private final float MotionPitch;
    private final float StopVolume;
    private final float StopPitch;
    private final boolean Layered;

    DoorVoice() {
        this(0.0f, 0.0f, 0.0f, 0.0f, true);
    }

    DoorVoice(float MotionVolume, float MotionPitch, float StopVolume, float StopPitch) {
        this(MotionVolume, MotionPitch, StopVolume, StopPitch, false);
    }

    DoorVoice(float MotionVolume, float MotionPitch, float StopVolume, float StopPitch, boolean Layered) {
        this.MotionVolume = MotionVolume;
        this.MotionPitch = MotionPitch;
        this.StopVolume = StopVolume;
        this.StopPitch = StopPitch;
        this.Layered = Layered;
    }

    public boolean isLayered() {
        return Layered;
    }

    private SoundEvent motion(boolean Opening) {
        return Opening ? DoorSounds.DoorOpen.get() : DoorSounds.DoorClose.get();
    }

    private SoundEvent halt() {
        return this == Heavy ? DoorSounds.HeavyClunk.get() : DoorSounds.DoorClunk.get();
    }

    public void start(ServerLevel World, Vec3 Centre, boolean Opening) {
        if (Layered) {
            return;
        }
        float Pitch = MotionPitch * (0.96f + World.getRandom().nextFloat() * 0.08f);
        World.playSound(null, Centre.x, Centre.y, Centre.z, motion(Opening), SoundSource.BLOCKS, MotionVolume, Pitch);
    }

    public void stop(ServerLevel World, Vec3 Centre, boolean Opened) {
        if (Layered) {
            return;
        }
        float Volume = Opened ? StopVolume * 0.5f : StopVolume;
        float Pitch = Opened ? StopPitch * 1.15f : StopPitch;
        World.playSound(null, Centre.x, Centre.y, Centre.z, halt(), SoundSource.BLOCKS, Volume, Pitch);
    }
}
