package net.rai.scp_unleashed.rifttech.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rift.RiftParticleOptions;
import net.rai.scp_unleashed.rift.RiftRegistry;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import net.rai.scp_unleashed.rifttech.GridStatus;
import net.rai.scp_unleashed.rifttech.InstabilityPylonBlock;
import net.rai.scp_unleashed.rifttech.InstabilityPylonBlockEntity;
import net.rai.scp_unleashed.rifttech.PylonGrid;
import net.rai.scp_unleashed.rifttech.PylonMode;
import net.rai.scp_unleashed.rifttech.RiftTechRegistry;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class PylonClientEffects {
    private static final Map<InstabilityPylonBlockEntity, Voice> Voices = new WeakHashMap<>();

    private PylonClientEffects() {
    }

    public static void tick(InstabilityPylonBlockEntity Pylon) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Pylon.getLevel();
        if (World == null || Game.level != World || Game.isPaused()) {
            return;
        }
        RandomSource Random = World.getRandom();
        Voice Current = Voices.computeIfAbsent(Pylon, Key -> new Voice());
        SoundManager Sounds = Game.getSoundManager();
        PylonMode Mode = mode(Pylon);
        int Age = Pylon.getClientTicks();
        if (Mode != PylonMode.Off && Mode != PylonMode.Idle) {
            if (CollectorClientEffects.lapsed(Sounds, Current.Hum, Age - Current.HumSince)) {
                Current.HumSince = Age;
                Current.Hum = new RiftTechLoopSound<>(Pylon, RiftTechRegistry.PylonHum.get(), PylonClientEffects::humLevel,
                        Owner -> 0.82 - 0.05 * Owner.getClientTier().getTier() + (mode(Owner) == PylonMode.Apocalyptic ? -0.12 : 0.0), InstabilityPylonBlockEntity::getEmitter);
                Sounds.play(Current.Hum);
            }
            if (CollectorClientEffects.lapsed(Sounds, Current.Bed, Age - Current.BedSince)) {
                Current.BedSince = Age;
                Current.Bed = new RiftTechLoopSound<>(Pylon, RiftTechRegistry.PylonHumBed.get(), Owner -> humLevel(Owner) * 0.6,
                        Owner -> mode(Owner) == PylonMode.Apocalyptic ? 0.62 : 0.78, InstabilityPylonBlockEntity::getEmitter);
                Sounds.play(Current.Bed);
            }
        }
        if (Mode.isCharging() && Random.nextFloat() < 0.04f) {
            Vec3 Emitter = Pylon.getEmitter();
            int Colour = Mode == PylonMode.Apocalyptic ? ApocalypticRiftEntity.Colour : Pylon.getClientTier().getColour();
            for (int Count = 0; Count < 3; Count++) {
                World.addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 0.7f), Emitter.x, Emitter.y, Emitter.z,
                        (Random.nextDouble() - 0.5) * 0.12, Random.nextDouble() * 0.08, (Random.nextDouble() - 0.5) * 0.12);
            }
        }
        if (!Pylon.isClientMaster()) {
            return;
        }
        PylonGrid.Layout Grid = Pylon.getClientLayout();
        GridStatus Status = Pylon.getClientStatus();
        if (Grid == null) {
            return;
        }
        if (Status.isCharging()) {
            if (--Current.ArcDelay <= 0) {
                Current.ArcDelay = 18 + Random.nextInt(40);
                List<BlockPos> Corners = Grid.corners();
                Vec3 A = InstabilityPylonBlockEntity.emitterOf(Corners.get(Random.nextInt(Corners.size())));
                Vec3 B = InstabilityPylonBlockEntity.emitterOf(Corners.get(Random.nextInt(Corners.size())));
                Vec3 Middle = A.add(B).scale(0.5);
                float Pitch = 1.0f + Random.nextFloat() * 0.3f;
                World.playLocalSound(Middle.x, Middle.y, Middle.z, RiftTechRegistry.PylonArc.get(), SoundSource.BLOCKS, 0.55f, Pitch, false);
                World.playLocalSound(Middle.x, Middle.y, Middle.z, RiftTechRegistry.PylonArcSnap.get(), SoundSource.BLOCKS, 0.4f, Pitch, false);
            }
        }
        if (Status == GridStatus.Apocalyptic) {
            if (CollectorClientEffects.lapsed(Sounds, Current.Drone, Age - Current.DroneSince)) {
                Current.DroneSince = Age;
                Current.Drone = new RiftTechLoopSound<>(Pylon, RiftTechRegistry.ApocalypticDrone.get(),
                        Owner -> Owner.getClientStatus() == GridStatus.Apocalyptic ? 0.35 + 0.65 * Owner.getClientApocalyptic() : 0.0,
                        Owner -> 0.55 + 0.5 * Owner.getClientApocalyptic(), PylonClientEffects::centre);
                Sounds.play(Current.Drone);
            }
            if (CollectorClientEffects.lapsed(Sounds, Current.DroneBed, Age - Current.DroneBedSince)) {
                Current.DroneBedSince = Age;
                Current.DroneBed = new RiftTechLoopSound<>(Pylon, RiftTechRegistry.ApocalypticDroneBed.get(),
                        Owner -> Owner.getClientStatus() == GridStatus.Apocalyptic ? 0.25 + 0.5 * Owner.getClientApocalyptic() : 0.0,
                        Owner -> 0.5 + 0.35 * Owner.getClientApocalyptic(), PylonClientEffects::centre);
                Sounds.play(Current.DroneBed);
            }
            rise(World, Grid, Pylon.getClientApocalyptic(), Random);
        }
        int Surge = Pylon.takePendingSurge();
        boolean Apocalypse = Pylon.takePendingApocalypse();
        if (Surge >= 0) {
            Vec3 Spot = Vec3.atLowerCornerOf(Pylon.getBlockPos()).add(InstabilityPylonBlockEntity.unpackOffset(Surge));
            int Colour = Apocalypse ? ApocalypticRiftEntity.Colour : InstabilityPylonBlockEntity.surgeTier(Surge).getColour();
            burst(World, Spot, Colour, Apocalypse ? 3.0f : 1.0f, Random);
        }
    }

    private static PylonMode mode(InstabilityPylonBlockEntity Pylon) {
        BlockState State = Pylon.getBlockState();
        return State.hasProperty(InstabilityPylonBlock.Mode) ? State.getValue(InstabilityPylonBlock.Mode) : PylonMode.Off;
    }

    private static double humLevel(InstabilityPylonBlockEntity Pylon) {
        return switch (mode(Pylon)) {
            case Apocalyptic -> 0.6;
            case Active -> 0.42;
            case Linked -> 0.16;
            default -> 0.0;
        };
    }

    private static Vec3 centre(InstabilityPylonBlockEntity Pylon) {
        PylonGrid.Layout Grid = Pylon.getClientLayout();
        return Grid == null ? Pylon.getEmitter() : Grid.centre();
    }

    private static void rise(Level World, PylonGrid.Layout Grid, float Charge, RandomSource Random) {
        AABB Volume = Grid.volume();
        int Count = Random.nextFloat() < 0.3f + 1.2f * Charge ? 1 + (Charge > 0.6f ? 1 : 0) : 0;
        for (int Index = 0; Index < Count; Index++) {
            double X = Volume.minX + Random.nextDouble() * Volume.getXsize();
            double Y = Volume.minY + Random.nextDouble() * Volume.getYsize() * 0.4;
            double Z = Volume.minZ + Random.nextDouble() * Volume.getZsize();
            World.addParticle(new RiftParticleOptions(RiftRegistry.RiftMote.get(), ApocalypticRiftEntity.Colour, -1, 0.9f + 0.6f * Charge), X, Y, Z,
                    0.0, 0.02 + 0.03 * Random.nextDouble(), 0.0);
        }
    }

    private static void burst(Level World, Vec3 Spot, int Colour, float Scale, RandomSource Random) {
        World.addParticle(new RiftParticleOptions(RiftRegistry.RiftShockwave.get(), Colour, -1, 1.2f * Scale), Spot.x, Spot.y, Spot.z, 0.0, 0.0, 0.0);
        int Sparks = Math.round(24 * Scale);
        for (int Count = 0; Count < Sparks; Count++) {
            Vec3 Heading = new Vec3(Random.nextGaussian(), Random.nextGaussian() * 0.8, Random.nextGaussian()).normalize();
            double Speed = 0.2 + Random.nextDouble() * 0.4 * Scale;
            World.addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 1.1f), Spot.x, Spot.y, Spot.z, Heading.x * Speed, Heading.y * Speed, Heading.z * Speed);
        }
        int Motes = Math.round(12 * Scale);
        for (int Count = 0; Count < Motes; Count++) {
            Vec3 Heading = new Vec3(Random.nextGaussian(), Random.nextGaussian() * 0.8, Random.nextGaussian()).normalize();
            double Speed = 0.06 + Random.nextDouble() * 0.15;
            World.addParticle(new RiftParticleOptions(RiftRegistry.RiftMote.get(), Colour, -1, 1.3f), Spot.x, Spot.y, Spot.z, Heading.x * Speed, Heading.y * Speed, Heading.z * Speed);
        }
    }

    private static final class Voice {
        private RiftTechLoopSound<InstabilityPylonBlockEntity> Hum;
        private RiftTechLoopSound<InstabilityPylonBlockEntity> Bed;
        private RiftTechLoopSound<InstabilityPylonBlockEntity> Drone;
        private RiftTechLoopSound<InstabilityPylonBlockEntity> DroneBed;
        private int HumSince;
        private int BedSince;
        private int DroneSince;
        private int DroneBedSince;
        private int ArcDelay;
    }
}
