package net.rai.scp_unleashed.steam.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.steam.SteamTurbineBlockEntity;

import java.lang.ref.WeakReference;

public class TurbineLoopSound extends AbstractTickableSoundInstance {
    public static final float Silent = 0.012f;

    private final WeakReference<SteamTurbineBlockEntity> Machine;
    private final Layer Kind;

    public TurbineLoopSound(SteamTurbineBlockEntity Machine, Layer Kind) {
        super(Kind.Sound(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Machine = new WeakReference<>(Machine);
        this.Kind = Kind;
        this.looping = true;
        this.delay = 0;
        this.volume = Kind.volumeFor(Machine.getSpin());
        this.pitch = Kind.pitchFor(Machine.getSpin());
        this.x = Machine.getBlockPos().getX() + 0.5;
        this.y = Machine.getBlockPos().getY() + 0.5;
        this.z = Machine.getBlockPos().getZ() + 0.5;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        SteamTurbineBlockEntity Current = Machine.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Spin = Current.getSpin();
        volume = Kind.volumeFor(Spin);
        pitch = Kind.pitchFor(Spin);
        if (Spin < Silent) {
            stop();
        }
    }

    public record Layer(SoundEvent Sound, float Loudness, float Onset, float LowPitch, float PitchRange) {
        public float volumeFor(float Spin) {
            return Loudness * Mth.clamp((Spin - Onset) / (1.0f - Onset), 0.0f, 1.0f);
        }

        public float pitchFor(float Spin) {
            return LowPitch + PitchRange * Spin;
        }
    }
}
