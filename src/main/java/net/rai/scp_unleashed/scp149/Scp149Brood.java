package net.rai.scp_unleashed.scp149;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class Scp149Brood {
    public static final int Quorum = 3;
    public static final double HuddleReach = 2.5;
    public static final int NestRadius = 3;
    public static final int NestAttempts = 24;
    public static final double[][] ExitRings = {{1.05, 0.15, 8}, {1.05, 0.95, 8}, {0.0, 1.0, 1}, {1.9, 0.3, 12}, {1.9, 1.2, 12}};
    public static final double[][] BurstRings = {{0.6, 0.05, 6}, {1.05, 0.05, 8}, {1.5, 0.05, 10}, {0.6, 0.6, 6}, {2.0, 0.05, 12}};
    public static final double FlingMin = 0.12;
    public static final double FlingMax = 0.22;
    public static final double FlingRise = 0.24;
    public static final double FallbackSpread = 0.12;

    private Scp149Brood() {
    }

    public static boolean canHoldCocoon(ServerLevel World, BlockPos Pos) {
        if (!World.isLoaded(Pos) || World.isOutsideBuildHeight(Pos)) {
            return false;
        }
        BlockState At = World.getBlockState(Pos);
        BlockPos Below = Pos.below();
        if (!At.canBeReplaced() || !At.getFluidState().isEmpty() || !World.getFluidState(Below).isEmpty() || !World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP)) {
            return false;
        }
        return World.getEntitiesOfClass(LivingEntity.class, new AABB(Pos), Found -> !(Found instanceof Scp149Larva) && Found.isAlive()).isEmpty();
    }

    public static BlockPos nestNear(ServerLevel World, BlockPos Origin, RandomSource Random) {
        if (canHoldCocoon(World, Origin)) {
            return Origin.immutable();
        }
        for (int Radius = NestRadius; Radius <= NestRadius * 2; Radius += NestRadius) {
            for (int Attempt = 0; Attempt < NestAttempts; Attempt++) {
                BlockPos Candidate = Origin.offset(Random.nextInt(Radius * 2 + 1) - Radius, Random.nextInt(5) - 2, Random.nextInt(Radius * 2 + 1) - Radius);
                if (canHoldCocoon(World, Candidate)) {
                    return Candidate.immutable();
                }
            }
        }
        return Origin.immutable();
    }

    public static int burstFrom(ServerLevel World, LivingEntity Bearer, UUID Brood, BlockPos Nest, int Count) {
        RandomSource Random = World.getRandom();
        Vec3 Feet = Bearer.position();
        EntityDimensions Size = Scp149Registry.Larva.get().getDimensions();
        List<Vec3> Spots = spots(World, Feet, Size, BurstRings, Count);
        int Born = 0;
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Spot = Index < Spots.size() ? Spots.get(Index)
                    : Feet.add((Random.nextDouble() - 0.5) * FallbackSpread * 2.0, 0.05, (Random.nextDouble() - 0.5) * FallbackSpread * 2.0);
            Vec3 Away = new Vec3(Spot.x - Feet.x, 0.0, Spot.z - Feet.z);
            if (Away.lengthSqr() < 1.0E-4) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                Away = new Vec3(Math.cos(Angle), 0.0, Math.sin(Angle));
            }
            double Strength = FlingMin + Random.nextDouble() * (FlingMax - FlingMin);
            Vec3 Push = Away.normalize().scale(Strength).add(0.0, FlingRise + Random.nextDouble() * 0.08, 0.0);
            if (spawnLarva(World, Spot, Brood, Nest, Push, MobSpawnType.EVENT) != null) {
                Born++;
                World.sendParticles(Scp149Registry.Mote.get(), Spot.x, Spot.y + 0.2, Spot.z, 5, 0.12, 0.12, 0.12, 0.05);
            }
        }
        return Born;
    }

    @Nullable
    public static Scp149Larva spawnLarva(ServerLevel World, Vec3 Spot, UUID Brood, BlockPos Nest, Vec3 Push, MobSpawnType Reason) {
        Scp149Larva Larva = Scp149Registry.Larva.get().create(World);
        if (Larva == null) {
            return null;
        }
        float Yaw = World.getRandom().nextFloat() * 360.0f;
        Larva.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Larva.setYBodyRot(Yaw);
        Larva.setYHeadRot(Yaw);
        Larva.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), Reason, null);
        Larva.adopt(Brood, Nest, Scp149World.now(World.getServer()));
        Larva.setDeltaMovement(Push);
        return World.addFreshEntity(Larva) ? Larva : null;
    }

    public static List<Scp149Larva> huddle(Scp149Larva Leader) {
        return Leader.level().getEntitiesOfClass(Scp149Larva.class, Leader.getBoundingBox().inflate(HuddleReach),
                Other -> Other != Leader && Other.isAlive() && Other.gathering() && Leader.brood().equals(Other.brood()));
    }

    public static Optional<BlockPos> cocoonSpot(ServerLevel World, BlockPos Centre) {
        if (canHoldCocoon(World, Centre)) {
            return Optional.of(Centre);
        }
        for (BlockPos Candidate : BlockPos.withinManhattan(Centre, 2, 1, 2)) {
            if (canHoldCocoon(World, Candidate)) {
                return Optional.of(Candidate.immutable());
            }
        }
        return Optional.empty();
    }

    public static boolean build(ServerLevel World, Scp149Larva Leader, List<Scp149Larva> Mates) {
        Optional<BlockPos> Spot = cocoonSpot(World, Leader.blockPosition());
        if (Spot.isEmpty()) {
            return false;
        }
        BlockPos Pos = Spot.get();
        World.setBlock(Pos, Scp149Registry.Cocoon.get().defaultBlockState(), Block.UPDATE_ALL);
        if (!(World.getBlockEntity(Pos) instanceof Scp149CocoonBlockEntity Cocoon)) {
            return false;
        }
        Cocoon.spin(Leader.brood());
        List<Scp149Larva> Spun = new ArrayList<>(Mates);
        Spun.add(Leader);
        for (Scp149Larva Larva : Spun) {
            absorb(World, Cocoon, Larva);
        }
        Vec3 Centre = Vec3.atBottomCenterOf(Pos);
        play(World, Centre, Scp149Registry.BuildLayers, 1.0f);
        World.sendParticles(Scp149Registry.Mote.get(), Centre.x, Centre.y + 0.4, Centre.z, 14, 0.3, 0.3, 0.3, 0.03);
        return true;
    }

    public static void absorb(ServerLevel World, Scp149CocoonBlockEntity Cocoon, Scp149Larva Larva) {
        Cocoon.addStrand();
        World.sendParticles(Scp149Registry.Mote.get(), Larva.getX(), Larva.getY() + 0.15, Larva.getZ(), 4, 0.1, 0.05, 0.1, 0.02);
        Larva.discard();
    }

    @Nullable
    public static Scp149CocoonBlockEntity cocoonFor(ServerLevel World, UUID Brood, BlockPos Centre, int Radius) {
        Scp149CocoonBlockEntity Best = null;
        double Closest = Double.MAX_VALUE;
        int Reach = SectionPos.blockToSectionCoord(Radius) + 1;
        ChunkPos Middle = new ChunkPos(Centre);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                if (!World.hasChunk(Middle.x + Dx, Middle.z + Dz)) {
                    continue;
                }
                LevelChunk Chunk = World.getChunk(Middle.x + Dx, Middle.z + Dz);
                for (BlockEntity Found : Chunk.getBlockEntities().values()) {
                    if (!(Found instanceof Scp149CocoonBlockEntity Cocoon) || (Brood != null && !Brood.equals(Cocoon.brood()))) {
                        continue;
                    }
                    double Distance = Found.getBlockPos().distSqr(Centre);
                    if (Distance <= (double) Radius * Radius && Distance < Closest) {
                        Closest = Distance;
                        Best = Cocoon;
                    }
                }
            }
        }
        return Best;
    }

    public static List<Scp149CocoonBlockEntity> cocoonsNear(ServerLevel World, BlockPos Centre, int Radius) {
        List<Scp149CocoonBlockEntity> Found = new ArrayList<>();
        int Reach = SectionPos.blockToSectionCoord(Radius) + 1;
        ChunkPos Middle = new ChunkPos(Centre);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                if (!World.hasChunk(Middle.x + Dx, Middle.z + Dz)) {
                    continue;
                }
                for (BlockEntity Entity : World.getChunk(Middle.x + Dx, Middle.z + Dz).getBlockEntities().values()) {
                    if (Entity instanceof Scp149CocoonBlockEntity Cocoon && Entity.getBlockPos().distSqr(Centre) <= (double) Radius * Radius) {
                        Found.add(Cocoon);
                    }
                }
            }
        }
        return Found;
    }

    public record Split(int Freed, int Waiting, boolean Opened) {
        public static final Split None = new Split(0, 0, false);
    }

    public static List<Vec3> exits(ServerLevel World, BlockPos Pos, int Wanted) {
        return spots(World, Vec3.atBottomCenterOf(Pos), Scp149Registry.Fly.get().getDimensions(), ExitRings, Wanted);
    }

    public static List<Vec3> spots(ServerLevel World, Vec3 Centre, EntityDimensions Size, double[][] Rings, int Wanted) {
        List<Vec3> Found = new ArrayList<>();
        List<AABB> Taken = new ArrayList<>();
        for (int Ring = 0; Ring < Rings.length && Found.size() < Wanted; Ring++) {
            double Reach = Rings[Ring][0];
            double Rise = Rings[Ring][1];
            int Slots = (int) Rings[Ring][2];
            double Turn = World.getRandom().nextDouble() * Math.PI * 2.0;
            for (int Slot = 0; Slot < Slots && Found.size() < Wanted; Slot++) {
                double Angle = Turn + Slot * Math.PI * 2.0 / Slots;
                Vec3 Spot = Centre.add(Math.cos(Angle) * Reach, Rise, Math.sin(Angle) * Reach);
                AABB Box = Size.makeBoundingBox(Spot);
                if (!World.noCollision(Box) || World.containsAnyLiquid(Box) || Taken.stream().anyMatch(Other -> Other.intersects(Box))) {
                    continue;
                }
                Found.add(Spot);
                Taken.add(Box);
            }
        }
        return Found;
    }

    public static Split hatch(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (!(World.getBlockEntity(Pos) instanceof Scp149CocoonBlockEntity Cocoon) || !State.is(Scp149Registry.Cocoon.get()) || Cocoon.burst()) {
            return Split.None;
        }
        Scp149World Data = Scp149World.get(World.getServer());
        int Waiting = Cocoon.waiting();
        int Room = Math.max(0, Scp149World.Cap - Data.count());
        List<Vec3> Spots = exits(World, Pos, Math.min(Waiting, Room));
        int Freed = 0;
        for (Vec3 Spot : Spots) {
            if (emerge(World, Spot)) {
                Freed++;
            }
        }
        if (Freed > 0) {
            Cocoon.release(Freed);
        }
        Vec3 Centre = Vec3.atBottomCenterOf(Pos);
        if (Cocoon.waiting() > 0) {
            if (Freed > 0) {
                World.sendParticles(Scp149Registry.Mote.get(), Centre.x, Centre.y + 0.6, Centre.z, 8 + Freed * 4, 0.3, 0.4, 0.3, 0.06);
                play(World, Centre, Scp149Registry.BurstLayers, 1.1f);
            }
            return new Split(Freed, Cocoon.waiting(), false);
        }
        Cocoon.markBurst();
        World.removeBlock(Pos, false);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Centre.x, Centre.y + 0.5, Centre.z, 30, 0.25, 0.35, 0.25, 0.12);
        World.sendParticles(Scp149Registry.Mote.get(), Centre.x, Centre.y + 0.6, Centre.z, 24, 0.3, 0.4, 0.3, 0.09);
        play(World, Centre, Scp149Registry.BurstLayers, 1.0f);
        return new Split(Freed, 0, true);
    }

    private static boolean emerge(ServerLevel World, Vec3 Spot) {
        Scp149Fly Fly = Scp149Registry.Fly.get().create(World);
        if (Fly == null) {
            return false;
        }
        float Yaw = World.getRandom().nextFloat() * 360.0f;
        Fly.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Fly.setYBodyRot(Yaw);
        Fly.setYHeadRot(Yaw);
        Fly.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.BREEDING, null);
        Fly.setDeltaMovement(0.0, 0.2, 0.0);
        Fly.newborn();
        return World.addFreshEntity(Fly);
    }
    public static void tear(ServerLevel World, BlockPos Pos, BlockState State) {
        Vec3 Centre = Vec3.atBottomCenterOf(Pos);
        World.sendParticles(Scp149Registry.Mote.get(), Centre.x, Centre.y + 0.4, Centre.z, 10, 0.25, 0.3, 0.25, 0.03);
        World.playSound(null, Centre.x, Centre.y, Centre.z, Scp149Registry.TearWet.get(), SoundSource.BLOCKS, 0.9f, 0.9f + World.getRandom().nextFloat() * 0.1f);
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch) {
        float Varied = Pitch * (0.95f + World.getRandom().nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, 1.0f, Varied);
        }
    }

}
