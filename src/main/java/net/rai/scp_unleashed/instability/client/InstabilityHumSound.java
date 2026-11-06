package net.rai.scp_unleashed.instability.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.instability.InstabilityReactorBlockEntity;
import net.rai.scp_unleashed.instability.InstabilityRegistry;

import java.lang.ref.WeakReference;

public class InstabilityHumSound extends AbstractTickableSoundInstance {
    public static final float Loudness = 0.75f;
    public static final float Silent = 0.01f;

    private final WeakReference<InstabilityReactorBlockEntity> Reactor;
    private float Swell;

    public InstabilityHumSound(InstabilityReactorBlockEntity Reactor) {
        super(InstabilityRegistry.Hum.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Reactor = new WeakReference<>(Reactor);
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001f;
        this.pitch = pitchFor(Reactor, 1.0f);
        Vec3 Centre = Reactor.getCoreCentre();
        this.x = Centre.x;
        this.y = Centre.y;
        this.z = Centre.z;
    }

    private static float pitchFor(InstabilityReactorBlockEntity Reactor, float PartialTick) {
        float Tier = Reactor.getEasedGrade();
        float Waver = 0.02f * (1.0f - Reactor.getEasedCalm()) * (float) Math.sin(Reactor.getTime(PartialTick) * 0.31);
        return 1.12f - 0.07f * Tier + Waver + Reactor.getFlicker() * 0.06f + 0.06f * Reactor.getSpin() - Reactor.getCollapse(PartialTick) * 0.35f;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        InstabilityReactorBlockEntity Current = Reactor.get();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Minecraft.getInstance().level) {
            stop();
            return;
        }
        float Target = Loudness * Mth.clamp(Current.getOpen(1.0f) * (0.55f + 0.45f * Current.getSpin()) + Current.getFlicker() * 0.25f, 0.0f, 1.2f);
        Swell += (Target - Swell) * 0.1f;
        volume = Math.max(Swell, 0.001f);
        pitch = Mth.clamp(pitchFor(Current, 1.0f), 0.5f, 2.0f);
        if (Target < Silent && Swell < Silent) {
            stop();
        }
    }
}
