package net.rai.scp_unleashed.scp075;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;

import java.util.Optional;

public final class Scp075Spawning {
    public static final int SearchAttempts = 32;
    public static final int VerticalLimit = 48;
    public static final int SpreadMin = 1;
    public static final int SpreadMax = 3;

    private Scp075Spawning() {
    }

    public static boolean roomy(ServerLevel World, BlockPos Pos) {
        AABB Box = Scp075Registry.Snail.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Pos));
        return World.noCollision(Box) && !World.containsAnyLiquid(Box);
    }

    public static boolean fits(ServerLevel World, BlockPos Pos) {
        return LeadPlanner.isDryStandable(World, Pos) && roomy(World, Pos);
    }

    public static Optional<BlockPos> surfaceSpot(ServerLevel World, BlockPos Centre, int MinDistance, int MaxDistance, RandomSource Random) {
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        BlockPos Best = null;
        int BestScore = Integer.MAX_VALUE;
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Low + Random.nextDouble() * (High - Low);
            int X = Centre.getX() + Mth.floor(Math.cos(Angle) * Radius);
            int Z = Centre.getZ() + Mth.floor(Math.sin(Angle) * Radius);
            if (!World.isLoaded(new BlockPos(X, Centre.getY(), Z))) {
                continue;
            }
            BlockPos Pos = LeadPlanner.surface(World, X, Z, Centre.getY());
            if (Math.abs(Pos.getY() - Centre.getY()) > VerticalLimit || !World.canSeeSky(Pos) || !fits(World, Pos)) {
                continue;
            }
            int Light = World.getBrightness(LightLayer.BLOCK, Pos);
            if (Light < BestScore) {
                BestScore = Light;
                Best = Pos.immutable();
            }
        }
        return Optional.ofNullable(Best);
    }

    public static int spawnGroup(ServerLevel World, Vec3 Spot, int Count, float Yaw, MobSpawnType Reason) {
        RandomSource Random = World.getRandom();
        int Spawned = 0;
        BlockPos Origin = BlockPos.containing(Spot);
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Place = Spot;
            if (Index > 0) {
                Optional<BlockPos> Near = LeadPlanner.standableNear(World, Origin, SpreadMin, SpreadMax, 2, Random);
                if (Near.isEmpty() || !roomy(World, Near.get())) {
                    continue;
                }
                Place = Vec3.atBottomCenterOf(Near.get());
            }
            Scp075Snail Snail = Scp075Registry.Snail.get().create(World);
            if (Snail == null) {
                continue;
            }
            float Facing = Yaw + (Random.nextFloat() - 0.5f) * 60.0f;
            Snail.moveTo(Place.x, Place.y, Place.z, Facing, 0.0f);
            Snail.setYBodyRot(Facing);
            Snail.setYHeadRot(Facing);
            if (!World.noCollision(Snail)) {
                continue;
            }
            Snail.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Place)), Reason, null);
            Snail.setPersistenceRequired();
            if (World.addFreshEntity(Snail)) {
                World.sendParticles(Scp075Registry.Drip.get(), Place.x, Place.y + 0.3, Place.z, 8, 0.3, 0.15, 0.3, 0.02);
                Spawned++;
            }
        }
        return Spawned;
    }
}
