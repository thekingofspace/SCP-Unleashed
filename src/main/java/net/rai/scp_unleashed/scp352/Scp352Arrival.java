package net.rai.scp_unleashed.scp352;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp352Arrival {
    public static final int Strict = 0;
    public static final int Lenient = 1;
    public static final int Forced = 2;
    public static final int Attempts = 40;
    public static final int TreeRadius = 5;
    public static final int TreeLow = -1;
    public static final int TreeHigh = 8;
    public static final int EdgeTrees = 8;
    public static final int NearTrees = 3;
    public static final int Vertical = 6;

    private Scp352Arrival() {
    }

    public static Optional<BlockPos> findSite(ServerLevel World, ServerPlayer Reader, int MinDistance, int MaxDistance, int Mode, RandomSource Random) {
        BlockPos Best = null;
        int BestScore = -1;
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = MinDistance + Random.nextDouble() * Math.max(0, MaxDistance - MinDistance);
            int X = Mth.floor(Reader.getX() + Math.cos(Angle) * Radius);
            int Z = Mth.floor(Reader.getZ() + Math.sin(Angle) * Radius);
            BlockPos Pos = ground(World, X, Z, Reader.getBlockY());
            if (Pos == null || !fits(World, Pos)) {
                continue;
            }
            int Score = score(World, Pos);
            if (Score > BestScore) {
                Best = Pos;
                BestScore = Score;
            }
            if (Score >= 3) {
                break;
            }
        }
        if (Best == null) {
            return Optional.empty();
        }
        int Needed = Mode == Strict ? 3 : Mode == Lenient ? 1 : 0;
        return BestScore >= Needed ? Optional.of(Best) : Optional.empty();
    }

    @Nullable
    public static BlockPos ground(ServerLevel World, int X, int Z, int ReaderY) {
        if (!World.isLoaded(new BlockPos(X, ReaderY, Z))) {
            return null;
        }
        if (World.dimensionType().hasCeiling()) {
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Candidate = new BlockPos(X, ReaderY + Dy, Z);
                if (LeadPlanner.isDryStandable(World, Candidate)) {
                    return Candidate;
                }
            }
            return null;
        }
        BlockPos Top = new BlockPos(X, World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z), Z);
        return LeadPlanner.isDryStandable(World, Top) ? Top : null;
    }

    public static boolean fits(ServerLevel World, BlockPos Pos) {
        AABB Box = AABB.ofSize(Vec3.atBottomCenterOf(Pos).add(0.0, Scp352Crone.LowHeight * 0.5 + 0.01, 0.0), Scp352Crone.LowWidth, Scp352Crone.LowHeight, Scp352Crone.LowWidth);
        return World.noCollision(Box) && !World.containsAnyLiquid(Box);
    }

    public static int trees(ServerLevel World, BlockPos Pos) {
        int Count = 0;
        BlockPos.MutableBlockPos Probe = new BlockPos.MutableBlockPos();
        for (int Dx = -TreeRadius; Dx <= TreeRadius; Dx++) {
            for (int Dz = -TreeRadius; Dz <= TreeRadius; Dz++) {
                for (int Dy = TreeLow; Dy <= TreeHigh; Dy++) {
                    Probe.set(Pos.getX() + Dx, Pos.getY() + Dy, Pos.getZ() + Dz);
                    BlockState State = World.getBlockState(Probe);
                    if (State.is(BlockTags.LOGS) || State.is(BlockTags.LEAVES)) {
                        Count++;
                    }
                }
            }
        }
        return Count;
    }

    public static int score(ServerLevel World, BlockPos Pos) {
        BlockState Below = World.getBlockState(Pos.below());
        boolean Natural = Below.is(BlockTags.DIRT) || Below.is(BlockTags.SAND) || Below.is(BlockTags.SNOW) || World.getBiome(Pos).is(BiomeTags.IS_FOREST);
        if (!Natural) {
            return 0;
        }
        int Trees = trees(World, Pos);
        boolean Open = World.canSeeSky(Pos.above());
        if (Open && Trees >= EdgeTrees) {
            return 3;
        }
        return Trees >= NearTrees ? 2 : 1;
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        Vec3 Spot = Vec3.atBottomCenterOf(Found.Pos());
        Vec3 Toward = Found.Investigator().position().subtract(Spot);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        return place(World, Spot, Yaw).isPresent();
    }

    public static Optional<Scp352Crone> place(ServerLevel World, Vec3 Spot, float Yaw) {
        Scp352Crone Creature = Scp352Registry.Crone.get().create(World);
        if (Creature == null) {
            return Optional.empty();
        }
        Creature.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Creature.setYHeadRot(Yaw);
        Creature.setYBodyRot(Yaw);
        if (!World.noCollision(Creature)) {
            return Optional.empty();
        }
        Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        if (!World.addFreshEntity(Creature)) {
            return Optional.empty();
        }
        World.playSound(null, Spot.x, Spot.y + 1.0, Spot.z, Scp352Registry.Mutter.get(), SoundSource.HOSTILE, 1.2f, 0.85f);
        World.playSound(null, Spot.x, Spot.y + 1.0, Spot.z, Scp352Registry.Cackle.get(), SoundSource.HOSTILE, 0.9f, 0.8f);
        return Optional.of(Creature);
    }
}
