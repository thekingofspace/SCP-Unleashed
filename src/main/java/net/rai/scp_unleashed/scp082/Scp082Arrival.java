package net.rai.scp_unleashed.scp082;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;

import java.util.Optional;

public final class Scp082Arrival {
    public static final int Attempts = 40;
    public static final int VerticalReach = 16;
    public static final int WaterReach = 2;
    public static final double ViewCone = 0.45;

    private Scp082Arrival() {
    }

    public static boolean arrive(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Pos = Found.Pos();
        return place(World, Vec3.atBottomCenterOf(Pos), waterYaw(World, Pos, World.getRandom())).isPresent();
    }

    public static Optional<Scp082Fernand> place(ServerLevel World, Vec3 Spot, float Yaw) {
        Scp082Fernand Fernand = Scp082Registry.Fernand.get().create(World);
        if (Fernand == null) {
            return Optional.empty();
        }
        Fernand.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Fernand.setYHeadRot(Yaw);
        Fernand.setYBodyRot(Yaw);
        if (!World.noCollision(Fernand)) {
            return Optional.empty();
        }
        Fernand.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        Fernand.setHunger(Scp082Fernand.SpawnHunger);
        if (!World.addFreshEntity(Fernand)) {
            return Optional.empty();
        }
        return Optional.of(Fernand);
    }

    public static float waterYaw(ServerLevel World, BlockPos Pos, RandomSource Random) {
        double Sx = 0.0;
        double Sz = 0.0;
        for (int Dx = -WaterReach - 1; Dx <= WaterReach + 1; Dx++) {
            for (int Dz = -WaterReach - 1; Dz <= WaterReach + 1; Dz++) {
                for (int Dy = -1; Dy <= 0; Dy++) {
                    if (World.getFluidState(Pos.offset(Dx, Dy, Dz)).is(FluidTags.WATER)) {
                        Sx += Dx;
                        Sz += Dz;
                    }
                }
            }
        }
        if (Sx * Sx + Sz * Sz < 1.0e-4) {
            return Random.nextFloat() * 360.0f;
        }
        return (float) (Mth.atan2(Sz, Sx) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public static Optional<BlockPos> findSpot(ServerLevel World, ServerPlayer Reader, int MinDistance, int MaxDistance, RandomSource Random) {
        for (int Pass = 0; Pass < 3; Pass++) {
            for (int Attempt = 0; Attempt < Attempts; Attempt++) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                double Radius = MinDistance + Random.nextDouble() * (MaxDistance - MinDistance);
                int X = Mth.floor(Reader.getX() + Math.cos(Angle) * Radius);
                int Z = Mth.floor(Reader.getZ() + Math.sin(Angle) * Radius);
                if (!World.hasChunkAt(new BlockPos(X, Reader.getBlockY(), Z))) {
                    continue;
                }
                BlockPos Spot = new BlockPos(X, World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z), Z);
                if (Math.abs(Spot.getY() - Reader.getBlockY()) > VerticalReach || !suitable(World, Spot, Pass < 2)) {
                    continue;
                }
                if (Pass == 0 && seen(World, Reader, Spot)) {
                    continue;
                }
                return Optional.of(Spot);
            }
        }
        return Optional.empty();
    }

    public static boolean suitable(ServerLevel World, BlockPos Spot, boolean Strict) {
        if (!LeadPlanner.isDryStandable(World, Spot) || !besideWater(World, Spot)) {
            return false;
        }
        if (Strict && !Scp082ShoreCondition.isShoreBiome(World, Spot)) {
            return false;
        }
        AABB Room = Scp082Registry.Fernand.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Spot));
        return World.noCollision(Room) && !World.containsAnyLiquid(Room);
    }

    public static boolean besideWater(ServerLevel World, BlockPos Spot) {
        for (int Dx = -WaterReach; Dx <= WaterReach; Dx++) {
            for (int Dz = -WaterReach; Dz <= WaterReach; Dz++) {
                if (Dx == 0 && Dz == 0) {
                    continue;
                }
                for (int Dy = -1; Dy <= 0; Dy++) {
                    if (World.getFluidState(Spot.offset(Dx, Dy, Dz)).is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean seen(ServerLevel World, ServerPlayer Reader, BlockPos Spot) {
        Vec3 Eye = Reader.getEyePosition();
        Vec3 Head = Vec3.atBottomCenterOf(Spot).add(0.0, Scp082Fernand.EyeHeight, 0.0);
        Vec3 Toward = Head.subtract(Eye).normalize();
        if (Toward.dot(Reader.getViewVector(1.0f)) < ViewCone) {
            return false;
        }
        HitResult Hit = World.clip(new ClipContext(Eye, Head, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader));
        return Hit.getType() == HitResult.Type.MISS;
    }
}
