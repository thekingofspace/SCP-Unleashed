package net.rai.scp_unleashed.rifttech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Extractable;
import net.rai.scp_unleashed.rift.RiftParticleOptions;
import net.rai.scp_unleashed.rift.RiftRegistry;
import net.rai.scp_unleashed.rifttech.RiftCollectorBlockEntity;
import net.rai.scp_unleashed.rifttech.RiftTechRegistry;

import java.util.Map;
import java.util.WeakHashMap;

public final class CollectorClientEffects {
    public static final float ServoThreshold = 2.2f;
    public static final int ServoGap = 14;

    private static final Map<RiftCollectorBlockEntity, Voice> Voices = new WeakHashMap<>();

    private CollectorClientEffects() {
    }

    public static void tick(RiftCollectorBlockEntity Collector) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Collector.getLevel();
        if (World == null || Game.level != World || Game.isPaused()) {
            return;
        }
        Voice Current = Voices.computeIfAbsent(Collector, Key -> new Voice());
        SoundManager Sounds = Game.getSoundManager();
        Vec3 Pivot = Collector.getPivot();
        if (Collector.isBeaming()) {
            CollectorBeams.track(Collector);
            if (lapsed(Sounds, Current.Beam, Collector.getClientTicks() - Current.BeamSince)) {
                Current.BeamSince = Collector.getClientTicks();
                Current.Beam = new RiftTechLoopSound<>(Collector, RiftTechRegistry.CollectorBeam.get(),
                        Owner -> Owner.isBeaming() ? 0.55 : 0.0, Owner -> 1.0 + 0.25 * progress(Owner), Owner -> Owner.getEmitter(1.0f));
                Sounds.play(Current.Beam);
            }
            if (lapsed(Sounds, Current.Bed, Collector.getClientTicks() - Current.BedSince)) {
                Current.BedSince = Collector.getClientTicks();
                Current.Bed = new RiftTechLoopSound<>(Collector, RiftTechRegistry.CollectorBeamBed.get(),
                        Owner -> Owner.isBeaming() ? 0.35 : 0.0, Owner -> 0.9 + 0.2 * progress(Owner), Owner -> Owner.getEmitter(1.0f));
                Sounds.play(Current.Bed);
            }
            sparks(World, Collector, World.getRandom());
        }
        Current.Quiet = Math.max(0, Current.Quiet - 1);
        if (Collector.getTurnSpeed() > ServoThreshold && Current.Quiet == 0) {
            Current.Quiet = ServoGap;
            float Pitch = 0.9f + Math.min(0.35f, Collector.getTurnSpeed() * 0.04f) + World.getRandom().nextFloat() * 0.08f;
            World.playLocalSound(Pivot.x, Pivot.y, Pivot.z, RiftTechRegistry.CollectorServo.get(), SoundSource.BLOCKS, 0.45f, Pitch, false);
            World.playLocalSound(Pivot.x, Pivot.y, Pivot.z, RiftTechRegistry.CollectorServoClick.get(), SoundSource.BLOCKS, 0.35f, Pitch, false);
        }
    }

    public static boolean lapsed(SoundManager Sounds, RiftTechLoopSound<?> Sound, int Age) {
        return Sound == null || Sound.isStopped() || (!Sounds.isActive(Sound) && Age > 40);
    }

    private static double progress(RiftCollectorBlockEntity Collector) {
        Level World = Collector.getLevel();
        if (World == null) {
            return 0.0;
        }
        Entity Target = World.getEntity(Collector.getClientTarget());
        return Target instanceof Extractable Rift ? Rift.getExtraction() : 0.0;
    }

    private static void sparks(Level World, RiftCollectorBlockEntity Collector, RandomSource Random) {
        if (Random.nextFloat() > 0.35f) {
            return;
        }
        Entity Target = World.getEntity(Collector.getClientTarget());
        if (!(Target instanceof Extractable Rift)) {
            return;
        }
        Vec3 Emitter = Collector.getEmitter(1.0f);
        Vec3 Toward = Target.getBoundingBox().getCenter().subtract(Emitter).normalize();
        double Speed = 0.04 + Random.nextDouble() * 0.05;
        World.addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Rift.getRiftColour(), -1, 0.7f), Emitter.x, Emitter.y, Emitter.z,
                Toward.x * Speed + (Random.nextDouble() - 0.5) * 0.04, Toward.y * Speed + (Random.nextDouble() - 0.5) * 0.04, Toward.z * Speed + (Random.nextDouble() - 0.5) * 0.04);
    }

    private static final class Voice {
        private RiftTechLoopSound<RiftCollectorBlockEntity> Beam;
        private RiftTechLoopSound<RiftCollectorBlockEntity> Bed;
        private int Quiet;
        private int BeamSince;
        private int BedSince;
    }
}
