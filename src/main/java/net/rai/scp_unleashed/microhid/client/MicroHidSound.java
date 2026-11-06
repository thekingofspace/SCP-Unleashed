package net.rai.scp_unleashed.microhid.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import java.util.Set;

public class MicroHidSound extends AbstractTickableSoundInstance {
    public static final float FadeIn = 0.25F;
    public static final float FadeOut = 0.34F;

    private final Entity Source;
    private final Set<MicroHidEffects.Phase> Keep;
    private final float Loudness;
    private final boolean Looping;
    private float Fade;

    public MicroHidSound(SoundEvent Sound, Entity Source, float Loudness, float Pitch, boolean Looping, Set<MicroHidEffects.Phase> Keep) {
        super(Sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.Source = Source;
        this.Keep = Keep;
        this.Loudness = Loudness;
        this.Looping = Looping;
        this.looping = Looping;
        this.delay = 0;
        this.pitch = Pitch;
        this.Fade = Looping ? 0.0F : 1.0F;
        this.volume = Loudness * Fade;
        follow();
    }

    private void follow() {
        this.x = Source.getX();
        this.y = Source.getEyeY();
        this.z = Source.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (Source.isRemoved()) {
            stop();
            return;
        }
        follow();
        boolean Wanted = Keep.contains(MicroHidEffects.phaseOf(Source));
        Fade = Wanted ? Math.min(1.0F, Fade + FadeIn) : Math.max(0.0F, Fade - FadeOut);
        volume = Loudness * Fade;
        if (!Wanted && Fade <= 0.0F) {
            stop();
        }
    }

    public boolean isLoop() {
        return Looping;
    }
}
