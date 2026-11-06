package net.rai.scp_unleashed.scp280;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Scp280Spots {
    public static final int Attempts = 28;
    public static final int ScanUp = 6;
    public static final int ScanDown = 9;
    public static final float ViewSpread = 62.0f;
    public static final double EyeLiftMin = 1.35;
    public static final double EyeLiftMax = 2.25;
    public static final double Jitter = 0.32;
    public static final double OtherPlayerClearance = 5.0;
    public static final double SightSlack = 0.6;
    public static final int RevealNear = 8;
    public static final int RevealFar = 14;
    public static final int ClusterTries = 10;

    private Scp280Spots() {
    }

    public static Optional<Vec3> eyeSpot(ServerLevel World, ServerPlayer Viewer, double Near, double Far, RandomSource Random) {
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            float Yaw = Viewer.getYRot() + (Random.nextFloat() * 2.0f - 1.0f) * ViewSpread;
            double Distance = Near + Random.nextDouble() * Math.max(0.0, Far - Near);
            double X = Viewer.getX() - Mth.sin(Yaw * Mth.DEG_TO_RAD) * Distance;
            double Z = Viewer.getZ() + Mth.cos(Yaw * Mth.DEG_TO_RAD) * Distance;
            Optional<Vec3> Found = eyeAt(World, Viewer, X, Z, Random);
            if (Found.isPresent()) {
                return Found;
            }
        }
        return Optional.empty();
    }

    public static Optional<Vec3> eyeAt(ServerLevel World, ServerPlayer Viewer, double X, double Z, RandomSource Random) {
        Optional<BlockPos> Floor = floorNear(World, BlockPos.containing(X, Viewer.getY(), Z));
        if (Floor.isEmpty()) {
            return Optional.empty();
        }
        BlockPos Stand = Floor.get();
        double Lift = EyeLiftMin + Random.nextDouble() * (EyeLiftMax - EyeLiftMin);
        Vec3 Point = new Vec3(Stand.getX() + 0.5 + (Random.nextDouble() - 0.5) * Jitter * 2.0, Stand.getY() + Lift,
                Stand.getZ() + 0.5 + (Random.nextDouble() - 0.5) * Jitter * 2.0);
        return eyeValid(World, Viewer, Point) ? Optional.of(Point) : Optional.empty();
    }

    public static boolean eyeValid(ServerLevel World, ServerPlayer Viewer, Vec3 Point) {
        BlockPos Cell = BlockPos.containing(Point);
        if (!World.isLoaded(Cell) || !open(World, Cell)) {
            return false;
        }
        if (Scp280Light.at(World, Cell) > Scp280Light.SpotLevel) {
            return false;
        }
        for (Player Other : World.players()) {
            if (Other != Viewer && Other.position().distanceTo(Point) < OtherPlayerClearance) {
                return false;
            }
        }
        return visible(World, Viewer, Point);
    }

    public static boolean visible(ServerLevel World, Player Viewer, Vec3 Point) {
        Vec3 Eye = Viewer.getEyePosition();
        BlockHitResult Hit = World.clip(new ClipContext(Eye, Point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Viewer));
        return Hit.getType() == HitResult.Type.MISS || Hit.getLocation().distanceTo(Point) < SightSlack;
    }

    public static boolean open(ServerLevel World, BlockPos Cell) {
        BlockState State = World.getBlockState(Cell);
        return State.getCollisionShape(World, Cell).isEmpty() && State.getFluidState().isEmpty();
    }

    public static Optional<BlockPos> floorNear(ServerLevel World, BlockPos Around) {
        for (int Dy = ScanUp; Dy >= -ScanDown; Dy--) {
            BlockPos Cell = Around.offset(0, Dy, 0);
            if (!World.isLoaded(Cell)) {
                return Optional.empty();
            }
            BlockPos Below = Cell.below();
            if (open(World, Cell) && open(World, Cell.above()) && !World.getBlockState(Below).getCollisionShape(World, Below).isEmpty()
                    && World.getBlockState(Below).getFluidState().isEmpty()) {
                return Optional.of(Cell);
            }
        }
        return Optional.empty();
    }

    public static List<Vec3> cluster(ServerLevel World, ServerPlayer Viewer, Vec3 First, int Count, RandomSource Random) {
        List<Vec3> Found = new ArrayList<>();
        Found.add(First);
        Vec3 Toward = First.subtract(Viewer.getEyePosition());
        Vec3 Across = new Vec3(-Toward.z, 0.0, Toward.x).normalize();
        for (int Try = 0; Try < ClusterTries * Count && Found.size() < Count; Try++) {
            double Side = (Random.nextBoolean() ? 1.0 : -1.0) * (0.9 + Random.nextDouble() * 2.4);
            double Rise = (Random.nextDouble() - 0.45) * 1.4;
            double Back = Random.nextDouble() * 2.0;
            Vec3 Candidate = First.add(Across.scale(Side)).add(0.0, Rise, 0.0).add(Toward.normalize().scale(Back));
            boolean Crowded = false;
            for (Vec3 Other : Found) {
                if (Other.distanceTo(Candidate) < 0.8) {
                    Crowded = true;
                    break;
                }
            }
            if (!Crowded && eyeValid(World, Viewer, Candidate)) {
                Found.add(Candidate);
            }
        }
        return Found;
    }

    public static Optional<Vec3> revealSpot(ServerLevel World, ServerPlayer Viewer, RandomSource Random) {
        for (int Pass = 0; Pass < 2; Pass++) {
            for (int Attempt = 0; Attempt < Attempts; Attempt++) {
                float Yaw = Viewer.getYRot() + (Pass == 0 ? (Random.nextFloat() * 2.0f - 1.0f) * ViewSpread : Random.nextFloat() * 360.0f);
                double Distance = RevealNear + Random.nextDouble() * (RevealFar - RevealNear);
                double X = Viewer.getX() - Mth.sin(Yaw * Mth.DEG_TO_RAD) * Distance;
                double Z = Viewer.getZ() + Mth.cos(Yaw * Mth.DEG_TO_RAD) * Distance;
                Optional<BlockPos> Floor = floorNear(World, BlockPos.containing(X, Viewer.getY(), Z));
                if (Floor.isEmpty()) {
                    continue;
                }
                Vec3 Spot = Vec3.atBottomCenterOf(Floor.get());
                if (!standable(World, Spot) || Scp280Light.at(World, Floor.get()) > Scp280Light.DarkLevel) {
                    continue;
                }
                if (Pass == 0 && !visible(World, Viewer, Spot.add(0.0, 1.6, 0.0))) {
                    continue;
                }
                return Optional.of(Spot);
            }
        }
        return Optional.empty();
    }

    public static boolean standable(ServerLevel World, Vec3 Spot) {
        AABB Room = Scp280Registry.Shade.get().getDimensions().makeBoundingBox(Spot);
        return World.noCollision(Room) && !World.containsAnyLiquid(Room);
    }
}
