package net.rai.scp_unleashed.scp327;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Scp327Shoal {
    public static final int ShoalSize = 3;
    public static final int MinDepth = 2;
    public static final int MinArea = 24;
    public static final int AreaLimit = 48;
    public static final int GridStep = 3;
    public static final int BandJitter = 64;
    public static final int BodyChecks = 24;
    public static final int VerticalReach = 24;
    public static final double Spacing = 2.0;
    public static final int PopulationRadius = 128;
    public static final int PopulationCap = 8;

    private Scp327Shoal() {
    }

    public static Optional<Scp327Waters.Body> findWater(ServerLevel World, Vec3 Origin, int MinDistance, int MaxDistance, RandomSource Random) {
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        int OriginX = Mth.floor(Origin.x);
        int OriginZ = Mth.floor(Origin.z);
        int Shift = Random.nextInt(GridStep);
        List<long[]> Candidates = new ArrayList<>();
        for (int Dx = -High + Shift; Dx <= High; Dx += GridStep) {
            for (int Dz = -High + Shift; Dz <= High; Dz += GridStep) {
                long Square = (long) Dx * Dx + (long) Dz * Dz;
                if (Square < (long) Low * Low || Square > (long) High * High) {
                    continue;
                }
                Candidates.add(new long[]{Square + Random.nextInt(BandJitter), OriginX + Dx, OriginZ + Dz});
            }
        }
        Candidates.sort((First, Second) -> Long.compare(First[0], Second[0]));
        int Checked = 0;
        for (long[] Candidate : Candidates) {
            int X = (int) Candidate[1];
            int Z = (int) Candidate[2];
            BlockPos Column = new BlockPos(X, Mth.floor(Origin.y), Z);
            if (!World.hasChunkAt(Column)) {
                continue;
            }
            int Top = World.getHeight(Heightmap.Types.WORLD_SURFACE, X, Z) - 1;
            BlockPos Surface = new BlockPos(X, Top, Z);
            if (!Scp327Waters.isWater(World, Surface) || Math.abs(Surface.getY() - Origin.y) > VerticalReach) {
                Surface = columnWater(World, X, Mth.floor(Origin.y), Z);
                if (Surface == null) {
                    continue;
                }
            }
            if (Math.abs(Surface.getY() - Origin.y) > VerticalReach) {
                continue;
            }
            Optional<Scp327Waters.Body> Found = Scp327Waters.body(World, Surface, MinDepth, MinArea, AreaLimit);
            if (Found.isPresent()) {
                return Found;
            }
            if (++Checked >= BodyChecks) {
                break;
            }
        }
        return Optional.empty();
    }

    private static BlockPos columnWater(ServerLevel World, int X, int Y, int Z) {
        for (int Dy = 8; Dy >= -8; Dy--) {
            BlockPos Pos = new BlockPos(X, Y + Dy, Z);
            if (Scp327Waters.isWater(World, Pos) && !Scp327Waters.isWater(World, Pos.above()) && World.getBlockState(Pos.above()).isAir()) {
                return Pos;
            }
        }
        return null;
    }

    public static List<Vec3> spots(ServerLevel World, Scp327Waters.Body Water, int Count, RandomSource Random) {
        List<BlockPos> Columns = new ArrayList<>(Water.Columns());
        List<Vec3> Chosen = new ArrayList<>();
        for (BlockPos Column : Columns) {
            if (Chosen.size() >= Count) {
                break;
            }
            int Depth = Scp327Waters.depthAt(World, Column.getX(), Column.getY(), Column.getZ());
            if (Depth < MinDepth) {
                continue;
            }
            double Y = Column.getY() - Math.min(1.6, Depth - 1.0) + 0.1;
            Vec3 Spot = new Vec3(Column.getX() + 0.5, Y, Column.getZ() + 0.5);
            boolean Crowded = false;
            for (Vec3 Other : Chosen) {
                if (Other.distanceTo(Spot) < Spacing) {
                    Crowded = true;
                    break;
                }
            }
            if (!Crowded) {
                Chosen.add(Spot);
            }
        }
        return Chosen;
    }

    public static List<Scp327Siren> spawn(ServerLevel World, Scp327Waters.Body Water, int Count, Vec3 Face, RandomSource Random) {
        List<Scp327Siren> Made = new ArrayList<>();
        for (Vec3 Spot : spots(World, Water, Count, Random)) {
            Scp327Siren Creature = Scp327Registry.Siren.get().create(World);
            if (Creature == null) {
                continue;
            }
            float Yaw = (float) (Mth.atan2(Face.z - Spot.z, Face.x - Spot.x) * Mth.RAD_TO_DEG) - 90.0f + (Random.nextFloat() - 0.5f) * 60.0f;
            Creature.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
            Creature.setYHeadRot(Yaw);
            Creature.setYBodyRot(Yaw);
            Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
            Creature.setPersistenceRequired();
            if (World.addFreshEntity(Creature)) {
                Made.add(Creature);
            }
        }
        return Made;
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        Optional<Scp327Waters.Body> Water = Scp327Waters.body(World, Found.Pos(), MinDepth, MinArea, AreaLimit);
        if (Water.isEmpty()) {
            return false;
        }
        List<Scp327Siren> Made = spawn(World, Water.get(), ShoalSize, Found.Investigator().position(), World.getRandom());
        for (Scp327Siren Creature : Made) {
            Creature.hunt(Found.Investigator(), Scp327Siren.PursuitTicks);
        }
        return !Made.isEmpty();
    }

    public static int population(ServerLevel World, Vec3 Centre) {
        return World.getEntitiesOfClass(Scp327Siren.class, new AABB(Centre, Centre).inflate(PopulationRadius), LivingEntity::isAlive).size();
    }

    public static boolean roomFor(ServerLevel World, Vec3 Centre) {
        return population(World, Centre) < PopulationCap;
    }
}
