package net.rai.scp_unleashed.scp343;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.furnishing.FurnishingSeat;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp343Thrones {
    public static final double Radius = 16.0;

    private Scp343Thrones() {
    }

    public static boolean isThrone(BlockState State) {
        return State.getBlock() instanceof Scp343ThroneBlock && State.getValue(Scp343ThroneBlock.Half) == DoubleBlockHalf.LOWER;
    }

    public static boolean formed(ServerLevel World, BlockPos Pos) {
        BlockState Lower = World.getBlockState(Pos);
        BlockState Upper = World.getBlockState(Pos.above());
        return isThrone(Lower) && Upper.getBlock() instanceof Scp343ThroneBlock && Upper.getValue(Scp343ThroneBlock.Half) == DoubleBlockHalf.UPPER;
    }

    public static boolean valid(ServerLevel World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            return true;
        }
        if (formed(World, Pos)) {
            return true;
        }
        Scp343Data.get(World.getServer()).removeThrone(World.dimension(), Pos);
        return false;
    }

    @Nullable
    public static BlockPos nearest(ServerLevel World, Vec3 From, double Reach) {
        BlockPos Best = null;
        double BestDistance = Reach * Reach;
        for (BlockPos Pos : Scp343Data.get(World.getServer()).thrones(World.dimension())) {
            double Distance = Vec3.atBottomCenterOf(Pos).distanceToSqr(From);
            if (Distance > BestDistance || !World.isLoaded(Pos) || !valid(World, Pos)) {
                continue;
            }
            BestDistance = Distance;
            Best = Pos;
        }
        return Best;
    }

    @Nullable
    public static BlockPos nearestLoaded(ServerLevel World, Vec3 From) {
        return nearest(World, From, Double.MAX_VALUE / 4.0);
    }

    public static boolean anyIn(MinecraftServer Server, ServerLevel World) {
        return !Scp343Data.get(Server).thrones(World.dimension()).isEmpty();
    }

    public static Direction facing(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.getBlock() instanceof Scp343ThroneBlock ? State.getValue(Scp343ThroneBlock.Facing) : Direction.NORTH;
    }

    public static Vec3 seat(ServerLevel World, BlockPos Pos) {
        return new Vec3(Pos.getX() + 0.5, Pos.getY() + Scp343ThroneBlock.SeatHeight, Pos.getZ() + 0.5);
    }

    public static BlockPos front(ServerLevel World, BlockPos Pos) {
        return Pos.relative(facing(World, Pos));
    }

    public static boolean occupied(ServerLevel World, BlockPos Pos) {
        List<FurnishingSeat> Seats = World.getEntitiesOfClass(FurnishingSeat.class, new AABB(Pos));
        for (FurnishingSeat Seat : Seats) {
            if (Seat.isVehicle()) {
                return true;
            }
        }
        return false;
    }

    public static boolean seatedOn(Entity Sitter, BlockPos Pos) {
        return Sitter.getVehicle() instanceof FurnishingSeat Seat && Seat.blockPosition().equals(Pos);
    }
}
