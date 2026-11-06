package net.rai.scp_unleashed.z0004;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class Z0004Fight {
    public static final int WaterRadius = 24;
    public static final int WaterDepthSpan = 12;
    public static final int SurfaceScan = 96;
    public static final int ScanCap = 24000;
    public static final int WaterNeeded = 1500;
    public static final int SpotMin = 30;
    public static final int SpotMax = 40;
    public static final int SpotStep = 3;
    public static final int SpotAngles = 24;
    public static final int HullDepth = 4;
    public static final double HullHalfLength = 10.0;
    public static final double HullHalfBeam = 2.5;
    public static final double SurfaceOffset = 0.88;
    public static final int FightTicks = 12000;
    public static final int LeaveTicks = 600;
    public static final double LeaveRange = 96.0;
    public static final double JoinRange = 32.0;
    public static final int CooldownTicks = 72000;
    public static final int RetryTicks = 1200;
    public static final double Leash = 56.0;
    public static final double CrewLeash = 48.0;

    private Z0004Fight() {
    }

    public record Spot(Vec3 Position, float Yaw, int Surface) {
    }

    public static boolean isWater(Level World, BlockPos Pos) {
        return World.getFluidState(Pos).is(FluidTags.WATER);
    }

    public static boolean touchesWater(Level World, BlockPos Pos) {
        if (isWater(World, Pos)) {
            return true;
        }
        for (Direction Side : Direction.values()) {
            if (isWater(World, Pos.relative(Side))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static BlockPos waterBeside(Level World, BlockPos Pos) {
        if (isWater(World, Pos)) {
            return Pos;
        }
        if (isWater(World, Pos.above())) {
            return Pos.above();
        }
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (isWater(World, Pos.relative(Side))) {
                return Pos.relative(Side);
            }
        }
        return isWater(World, Pos.below()) ? Pos.below() : null;
    }

    public static int surfaceAbove(Level World, BlockPos Pos) {
        BlockPos Start = waterBeside(World, Pos);
        if (Start == null) {
            return Integer.MIN_VALUE;
        }
        BlockPos.MutableBlockPos Cursor = Start.mutable();
        int Top = Integer.MIN_VALUE;
        for (int Step = 0; Step < SurfaceScan; Step++) {
            if (!World.isLoaded(Cursor)) {
                return Integer.MIN_VALUE;
            }
            if (isWater(World, Cursor)) {
                Top = Cursor.getY();
                Cursor.move(Direction.UP);
                continue;
            }
            return World.getBlockState(Cursor).isAir() ? Top : Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    public static int connectedWater(Level World, BlockPos Pos, int Limit) {
        return connectedWater(World, Pos, surfaceAbove(World, Pos), Limit);
    }

    public static int connectedWater(Level World, BlockPos Pos, int Surface, int Limit) {
        if (Surface == Integer.MIN_VALUE) {
            return 0;
        }
        int Lowest = Math.min(Pos.getY() - WaterDepthSpan, Surface - WaterRadius);
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        Set<Long> Seen = new HashSet<>();
        BlockPos Start = waterBeside(World, Pos);
        if (Start != null && Seen.add(Start.asLong())) {
            Queue.add(Start);
        }
        int Count = 0;
        while (!Queue.isEmpty() && Count < Limit && Seen.size() < ScanCap) {
            BlockPos Here = Queue.poll();
            Count++;
            for (Direction Side : Direction.values()) {
                BlockPos Next = Here.relative(Side);
                if (Math.abs(Next.getX() - Pos.getX()) > WaterRadius || Math.abs(Next.getZ() - Pos.getZ()) > WaterRadius || Next.getY() > Surface || Next.getY() < Lowest) {
                    continue;
                }
                if (!World.isLoaded(Next) || !Seen.add(Next.asLong())) {
                    continue;
                }
                if (isWater(World, Next)) {
                    Queue.add(Next);
                }
            }
        }
        return Count;
    }

    public static boolean largeWater(Level World, BlockPos Pos) {
        int Surface = surfaceAbove(World, Pos);
        return Surface != Integer.MIN_VALUE && connectedWater(World, Pos, Surface, WaterNeeded) >= WaterNeeded;
    }

    public static Vec3 anchor(Level World, BlockPos Pos) {
        int Surface = surfaceAbove(World, Pos);
        return new Vec3(Pos.getX() + 0.5, Surface == Integer.MIN_VALUE ? Pos.getY() + 0.5 : Surface + 1.0, Pos.getZ() + 0.5);
    }

    public static double flatDistanceSqr(Vec3 First, Vec3 Second) {
        double Dx = First.x - Second.x;
        double Dz = First.z - Second.z;
        return Dx * Dx + Dz * Dz;
    }

    public static int surface(Level World, int X, int Z, int FromY) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, FromY, Z);
        for (int Y = FromY; Y > FromY - 24; Y--) {
            Cursor.setY(Y);
            if (!World.isLoaded(Cursor)) {
                return Integer.MIN_VALUE;
            }
            if (isWater(World, Cursor)) {
                Cursor.setY(Y + 1);
                return World.getBlockState(Cursor).isAir() ? Y : Integer.MIN_VALUE;
            }
        }
        return Integer.MIN_VALUE;
    }

    public static boolean deepAt(Level World, double X, double Z, int Surface) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(Mth.floor(X), Surface, Mth.floor(Z));
        if (!World.isLoaded(Cursor)) {
            return false;
        }
        for (int Depth = 0; Depth < HullDepth; Depth++) {
            Cursor.setY(Surface - Depth);
            if (!isWater(World, Cursor)) {
                return false;
            }
        }
        Cursor.setY(Surface + 1);
        return !World.getBlockState(Cursor).isSolid();
    }

    public static boolean hullFits(Level World, Vec3 Centre, float Yaw, int Surface) {
        Vec3 Forward = forward(Yaw);
        Vec3 Left = left(Yaw);
        for (double Along = -HullHalfLength; Along <= HullHalfLength + 1.0e-6; Along += HullHalfLength / 2.0) {
            for (double Across = -HullHalfBeam; Across <= HullHalfBeam + 1.0e-6; Across += HullHalfBeam) {
                Vec3 Point = Centre.add(Forward.scale(Along)).add(Left.scale(Across));
                if (!deepAt(World, Point.x, Point.z, Surface)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Optional<Spot> findSpot(Level World, BlockPos Chest) {
        int Water = surfaceAbove(World, Chest);
        if (Water == Integer.MIN_VALUE) {
            return Optional.empty();
        }
        int Top = Water + 2;
        for (int Distance = SpotMin; Distance <= SpotMax; Distance += SpotStep) {
            for (int Index = 0; Index < SpotAngles; Index++) {
                double Angle = Math.PI * 2.0 * Index / SpotAngles;
                double X = Chest.getX() + 0.5 + Math.cos(Angle) * Distance;
                double Z = Chest.getZ() + 0.5 + Math.sin(Angle) * Distance;
                int Surface = surface(World, Mth.floor(X), Mth.floor(Z), Top);
                if (Surface == Integer.MIN_VALUE) {
                    continue;
                }
                Vec3 Centre = new Vec3(X, Surface + SurfaceOffset, Z);
                Vec3 Towards = new Vec3(Chest.getX() + 0.5 - Centre.x, 0.0, Chest.getZ() + 0.5 - Centre.z);
                float Yaw = yawOf(new Vec3(-Towards.z, 0.0, Towards.x));
                if (hullFits(World, Centre, Yaw, Surface)) {
                    return Optional.of(new Spot(Centre, Yaw, Surface));
                }
                if (hullFits(World, Centre, Yaw + 180.0f, Surface)) {
                    return Optional.of(new Spot(Centre, Yaw + 180.0f, Surface));
                }
            }
        }
        return Optional.empty();
    }

    public static Vec3 forward(float Yaw) {
        float Radians = Yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(Radians), 0.0, Mth.cos(Radians));
    }

    public static Vec3 left(float Yaw) {
        float Radians = Yaw * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(Radians), 0.0, Mth.sin(Radians));
    }

    public static float yawOf(Vec3 Direction) {
        return (float) (Mth.atan2(Direction.z, Direction.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public static boolean targetable(@Nullable Entity Target) {
        if (!(Target instanceof LivingEntity Living) || !Living.isAlive() || Target instanceof Z0004Pirate || Target instanceof Z0004Ship) {
            return false;
        }
        if (Target instanceof Player User) {
            return !User.isCreative() && !User.isSpectator();
        }
        return ControlBodies.isPersonLike(Target);
    }

    public static boolean fleet(@Nullable Entity Other) {
        return Other instanceof Z0004Pirate || Other instanceof Z0004Ship || Other instanceof Z0004ShipPart || Other instanceof Z0004Cannonball || Other instanceof Z0004Shot;
    }

    @Nullable
    public static Z0004ChestBlockEntity chestAt(ServerLevel World, @Nullable BlockPos Pos) {
        if (Pos == null || !World.isLoaded(Pos)) {
            return null;
        }
        return World.getBlockEntity(Pos) instanceof Z0004ChestBlockEntity Chest ? Chest : null;
    }
}
