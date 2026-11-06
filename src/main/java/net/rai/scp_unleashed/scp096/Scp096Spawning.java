package net.rai.scp_unleashed.scp096;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;

import java.util.Optional;

public final class Scp096Spawning {
    public static final int SearchAttempts = 48;
    public static final int Vertical = 10;
    public static final int DarkLimit = 7;
    public static final int SkyPenalty = 2;

    private Scp096Spawning() {
    }

    public static boolean roomy(ServerLevel World, BlockPos Cell) {
        AABB Box = Scp096Registry.ShyGuy.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Cell));
        return World.noCollision(Box) && !World.containsAnyLiquid(Box);
    }

    public static int darkness(ServerLevel World, BlockPos Cell) {
        return World.getMaxLocalRawBrightness(Cell);
    }

    public static Optional<BlockPos> darkSpot(ServerLevel World, BlockPos Centre, int MinDistance, int MaxDistance, RandomSource Random) {
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        BlockPos Best = null;
        int BestScore = Integer.MAX_VALUE;
        BlockPos Fallback = null;
        int FallbackScore = Integer.MAX_VALUE;
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Low + Random.nextDouble() * (High - Low);
            BlockPos Column = Centre.offset(Mth.floor(Math.cos(Angle) * Radius), 0, Mth.floor(Math.sin(Angle) * Radius));
            if (!World.isLoaded(Column)) {
                continue;
            }
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Cell = Column.above(Dy);
                if (!LeadPlanner.isDryStandable(World, Cell) || !roomy(World, Cell)) {
                    continue;
                }
                int Light = darkness(World, Cell);
                int Score = Light + (World.canSeeSky(Cell) ? SkyPenalty : 0) + Math.abs(Dy) / 4;
                if (Light <= DarkLimit && Score < BestScore) {
                    BestScore = Score;
                    Best = Cell.immutable();
                }
                if (Score < FallbackScore) {
                    FallbackScore = Score;
                    Fallback = Cell.immutable();
                }
            }
        }
        return Optional.ofNullable(Best != null ? Best : Fallback);
    }

    public static Optional<Scp096ShyGuy> place(ServerLevel World, Vec3 Spot, float Yaw, MobSpawnType Reason) {
        Scp096ShyGuy Guy = Scp096Registry.ShyGuy.get().create(World);
        if (Guy == null) {
            return Optional.empty();
        }
        Guy.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Guy.setYBodyRot(Yaw);
        Guy.setYHeadRot(Yaw);
        if (!World.noCollision(Guy)) {
            return Optional.empty();
        }
        Guy.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), Reason, null);
        Guy.setPersistenceRequired();
        return World.addFreshEntity(Guy) ? Optional.of(Guy) : Optional.empty();
    }

    public static float awayFrom(Vec3 Spot, Vec3 Watcher) {
        Vec3 Toward = Watcher.subtract(Spot);
        return (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f + 180.0f;
    }
}
