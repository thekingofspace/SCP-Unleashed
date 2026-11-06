package net.rai.scp_unleashed.scp007;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;

import java.util.Optional;

public final class Scp007Arrival {
    public static final int Attempts = 36;
    public static final int VerticalReach = 16;
    public static final double ViewCone = 0.45;
    public static final double ThreatClearance = 10.0;
    public static final float FacingSpread = 120.0f;

    private Scp007Arrival() {
    }

    public static boolean arrive(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Pos = Found.Pos();
        Vec3 Toward = Found.Investigator().position().subtract(Vec3.atBottomCenterOf(Pos));
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f + (World.getRandom().nextFloat() - 0.5f) * FacingSpread;
        return place(World, Vec3.atBottomCenterOf(Pos), Yaw, true).isPresent();
    }

    public static Optional<Scp007Entity> place(ServerLevel World, Vec3 Spot, float Yaw, boolean Seated) {
        Scp007Entity Subject = Scp007Registry.Subject.get().create(World);
        if (Subject == null) {
            return Optional.empty();
        }
        Subject.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Subject.setYHeadRot(Yaw);
        Subject.setYBodyRot(Yaw);
        if (!World.noCollision(Subject)) {
            return Optional.empty();
        }
        Subject.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        if (Seated) {
            Subject.sitDown(Scp007Entity.ArrivalGaze);
        }
        if (!World.addFreshEntity(Subject)) {
            return Optional.empty();
        }
        return Optional.of(Subject);
    }

    public static Optional<BlockPos> findSpot(ServerLevel World, ServerPlayer Reader, int MinDistance, int MaxDistance, RandomSource Random) {
        for (int Pass = 0; Pass < 2; Pass++) {
            for (int Attempt = 0; Attempt < Attempts; Attempt++) {
                double Angle = Random.nextDouble() * Math.PI * 2.0;
                double Radius = MinDistance + Random.nextDouble() * (MaxDistance - MinDistance);
                int X = Mth.floor(Reader.getX() + Math.cos(Angle) * Radius);
                int Z = Mth.floor(Reader.getZ() + Math.sin(Angle) * Radius);
                if (!World.hasChunkAt(new BlockPos(X, Reader.getBlockY(), Z))) {
                    continue;
                }
                BlockPos Spot = new BlockPos(X, World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z), Z);
                if (Math.abs(Spot.getY() - Reader.getBlockY()) > VerticalReach || !suitable(World, Spot)) {
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

    public static boolean suitable(ServerLevel World, BlockPos Spot) {
        if (!LeadPlanner.isDryStandable(World, Spot)) {
            return false;
        }
        AABB Room = Scp007Registry.Subject.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Spot));
        if (!World.noCollision(Room) || World.containsAnyLiquid(Room.inflate(1.0, 0.0, 1.0))) {
            return false;
        }
        return World.getEntitiesOfClass(LivingEntity.class, Room.inflate(ThreatClearance), Scp007Entity::isThreat).isEmpty();
    }

    public static boolean seen(ServerLevel World, ServerPlayer Reader, BlockPos Spot) {
        Vec3 Eye = Reader.getEyePosition();
        Vec3 Head = Vec3.atBottomCenterOf(Spot).add(0.0, 1.0, 0.0);
        Vec3 Toward = Head.subtract(Eye).normalize();
        if (Toward.dot(Reader.getViewVector(1.0f)) < ViewCone) {
            return false;
        }
        HitResult Hit = World.clip(new ClipContext(Eye, Head, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader));
        return Hit.getType() == HitResult.Type.MISS;
    }
}
