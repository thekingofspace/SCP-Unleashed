package net.rai.scp_unleashed.scp001;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class Scp001Warp {
    public static final double NearTarget = 2.5;
    public static final double FarTarget = 4.5;
    public static final double MinJump = 3.0;
    public static final double MaxJump = 12.0;
    public static final int Attempts = 28;
    public static final int Rise = 2;
    public static final int Drop = 3;
    public static final double BehindArc = 60.0;
    public static final double SideArc = 25.0;

    private Scp001Warp() {
    }

    public static Optional<Vec3> find(ServerLevel World, Scp001Prototype Hunter, LivingEntity Target, RandomSource Random) {
        float Facing = Target.getYHeadRot();
        BlockPos Centre = Target.blockPosition();
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Yaw = Attempt % 2 == 0 ? Facing + 180.0 + (Random.nextDouble() * 2.0 - 1.0) * BehindArc
                    : Facing + (Random.nextBoolean() ? 90.0 : -90.0) + (Random.nextDouble() * 2.0 - 1.0) * SideArc;
            double Radius = NearTarget + Random.nextDouble() * (FarTarget - NearTarget);
            double Dx = -Mth.sin((float) Math.toRadians(Yaw)) * Radius;
            double Dz = Mth.cos((float) Math.toRadians(Yaw)) * Radius;
            BlockPos Column = BlockPos.containing(Target.getX() + Dx, Target.getY(), Target.getZ() + Dz);
            for (int Dy = Rise; Dy >= -Drop; Dy--) {
                BlockPos Candidate = Column.above(Dy);
                if (!fits(World, Hunter, Candidate)) {
                    continue;
                }
                Vec3 Spot = Vec3.atBottomCenterOf(Candidate);
                double Jump = Spot.distanceTo(Hunter.position());
                if (Jump >= MinJump && Jump <= MaxJump && sees(World, Spot.add(0.0, Scp001Prototype.EyeHeight, 0.0), Target.getEyePosition())) {
                    return Optional.of(Spot);
                }
                break;
            }
        }
        return Optional.empty();
    }

    public static AABB roomAt(Vec3 Bottom) {
        double Half = Scp001Prototype.Width / 2.0;
        return new AABB(Bottom.x - Half, Bottom.y, Bottom.z - Half, Bottom.x + Half, Bottom.y + Scp001Prototype.Height, Bottom.z + Half);
    }

    public static boolean fits(ServerLevel World, Scp001Prototype Hunter, BlockPos Pos) {
        if (!World.isLoaded(Pos) || !World.isLoaded(Pos.above(2)) || World.isOutsideBuildHeight(Pos)) {
            return false;
        }
        BlockState Below = World.getBlockState(Pos.below());
        if (!Below.isFaceSturdy(World, Pos.below(), Direction.UP)) {
            return false;
        }
        AABB Room = roomAt(Vec3.atBottomCenterOf(Pos));
        if (!World.noCollision(Hunter, Room) || World.containsAnyLiquid(Room)) {
            return false;
        }
        PathType Type = WalkNodeEvaluator.getPathTypeStatic(Hunter, Pos);
        return Type == PathType.WALKABLE || Type == PathType.OPEN || Hunter.getPathfindingMalus(Type) == 0.0f;
    }

    public static boolean sees(ServerLevel World, Vec3 From, Vec3 To) {
        return World.clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    public static void trail(ServerLevel World, Vec3 From, Vec3 To) {
        Vec3 Step = To.subtract(From);
        int Count = Math.max(4, (int) (Step.length() * 3.0));
        for (int Index = 0; Index <= Count; Index++) {
            Vec3 Point = From.add(Step.scale(Index / (double) Count)).add(0.0, 1.0, 0.0);
            World.sendParticles(new DustParticleOptions(Scp001Prototype.Gold, 0.8f), Point.x, Point.y, Point.z, 1, 0.12, 0.35, 0.12, 0.0);
        }
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, From.x, From.y + 1.0, From.z, 14, 0.35, 0.7, 0.35, 0.15);
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, To.x, To.y + 1.0, To.z, 14, 0.35, 0.7, 0.35, 0.15);
        World.sendParticles(new DustParticleOptions(Scp001Prototype.Gold, 1.3f), To.x, To.y + 1.0, To.z, 10, 0.3, 0.6, 0.3, 0.0);
    }
}
