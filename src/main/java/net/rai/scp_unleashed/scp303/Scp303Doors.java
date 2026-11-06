package net.rai.scp_unleashed.scp303;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class Scp303Doors {
    public static final double SlabDepth = 3.0 / 16.0;
    public static final double ShoulderHalf = 0.72;
    public static final double ShoulderLow = 0.75;
    public static final double ShoulderHigh = 1.9;
    public static final double[] Retreats = {0.0, 0.2, 0.45};
    public static final double[] CellDepths = {2.5, 3.2, 1.9, 4.0};
    public static final int[] CellLaterals = {0, -1, 1};

    private Scp303Doors() {
    }

    public static boolean usable(BlockState State) {
        return DoorBlock.isWoodenDoor(State) && !State.is(Scp303Registry.IgnoredDoors);
    }

    public static boolean usable(Level World, BlockPos Lower) {
        BlockState State = World.getBlockState(Lower);
        return usable(State) && State.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && World.getBlockState(Lower.above()).is(State.getBlock());
    }

    public static Optional<BlockPos> lowerOf(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (!usable(State)) {
            return Optional.empty();
        }
        BlockPos Lower = State.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? Pos.below() : Pos.immutable();
        return usable(World, Lower) ? Optional.of(Lower) : Optional.empty();
    }

    public static boolean isOpen(Level World, BlockPos Lower) {
        BlockState State = World.getBlockState(Lower);
        return State.hasProperty(DoorBlock.OPEN) && State.getValue(DoorBlock.OPEN);
    }

    public static boolean shut(Level World, BlockPos Lower) {
        BlockState State = World.getBlockState(Lower);
        if (!(State.getBlock() instanceof DoorBlock Door) || !State.getValue(DoorBlock.OPEN)) {
            return false;
        }
        Door.setOpen(null, World, State, Lower, false);
        return true;
    }

    public static Direction slab(BlockState State) {
        return State.getValue(DoorBlock.FACING).getOpposite();
    }

    public static Direction.Axis axis(BlockState State) {
        return State.getValue(DoorBlock.FACING).getAxis();
    }

    public static Direction.Axis across(BlockState State) {
        return axis(State) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    public static double plane(BlockPos Lower, BlockState State) {
        Direction Edge = slab(State);
        double Base = Lower.get(Edge.getAxis());
        return Edge.getAxisDirection() == Direction.AxisDirection.POSITIVE ? Base + 1.0 - SlabDepth / 2.0 : Base + SlabDepth / 2.0;
    }

    public static double offset(BlockPos Lower, BlockState State, Vec3 Point) {
        return Point.get(axis(State)) - plane(Lower, State);
    }

    public static int sideOf(BlockPos Lower, BlockState State, Vec3 Point) {
        return offset(Lower, State, Point) >= 0.0 ? 1 : -1;
    }

    public static boolean insideSide(BlockState State, int Side) {
        int Edge = slab(State).getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        return Side == -Edge;
    }

    public static Vec3 centre(BlockPos Lower) {
        return Vec3.atCenterOf(Lower).add(0.0, 0.5, 0.0);
    }

    public static Vec3 handle(BlockPos Lower, BlockState State) {
        Direction.Axis Axis = axis(State);
        double Along = plane(Lower, State);
        Vec3 Middle = centre(Lower).add(0.0, -0.05, 0.0);
        return Axis == Direction.Axis.X ? new Vec3(Along, Middle.y, Middle.z) : new Vec3(Middle.x, Middle.y, Along);
    }

    public static Vec3 along(BlockPos Lower, BlockState State, double Coordinate, double Lateral, double Y) {
        Vec3 Middle = Vec3.atBottomCenterOf(Lower);
        if (axis(State) == Direction.Axis.X) {
            return new Vec3(Coordinate, Y, Middle.z + Lateral);
        }
        return new Vec3(Middle.x + Lateral, Y, Coordinate);
    }

    public static double reachFor(BlockState State, int Side) {
        return insideSide(State, Side) ? Scp303Doorman.PeekInside : Scp303Doorman.PeekReach;
    }

    public static Vec3 holdSpot(BlockPos Lower, BlockState State, int Side, double Back) {
        double Face = plane(Lower, State) + Side * SlabDepth / 2.0;
        return along(Lower, State, Face + Side * (reachFor(State, Side) + Back), 0.0, Lower.getY());
    }

    public static float facingYaw(BlockState State, int Side) {
        Direction Toward = Direction.fromAxisAndDirection(axis(State), Side > 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
        return Toward.toYRot();
    }

    public static AABB crouchBox(Vec3 Spot) {
        double Half = Scp303Doorman.CrouchWidth / 2.0;
        return new AABB(Spot.x - Half, Spot.y, Spot.z - Half, Spot.x + Half, Spot.y + Scp303Doorman.CrouchHeight, Spot.z + Half);
    }

    public static AABB standBox(Vec3 Spot) {
        double Half = Scp303Doorman.Width / 2.0;
        return new AABB(Spot.x - Half, Spot.y, Spot.z - Half, Spot.x + Half, Spot.y + Scp303Doorman.Height, Spot.z + Half);
    }

    public static AABB shoulderBox(BlockPos Lower, BlockState State, Vec3 Spot, int Side) {
        Vec3 Back = Spot.add(axis(State) == Direction.Axis.X ? Side * 0.25 : 0.0, 0.0, axis(State) == Direction.Axis.Z ? Side * 0.25 : 0.0);
        double HalfX = axis(State) == Direction.Axis.X ? 0.3 : ShoulderHalf;
        double HalfZ = axis(State) == Direction.Axis.Z ? 0.3 : ShoulderHalf;
        return new AABB(Back.x - HalfX, Back.y + ShoulderLow, Back.z - HalfZ, Back.x + HalfX, Back.y + ShoulderHigh, Back.z + HalfZ);
    }

    public static boolean grounded(Level World, Vec3 Spot) {
        BlockPos Under = BlockPos.containing(Spot.x, Spot.y - 0.5, Spot.z);
        return World.getBlockState(Under).isFaceSturdy(World, Under, Direction.UP) || !World.getBlockState(Under).getCollisionShape(World, Under).isEmpty();
    }

    public static Optional<Vec3> hold(Level World, BlockPos Lower, int Side) {
        BlockState State = World.getBlockState(Lower);
        if (!usable(State)) {
            return Optional.empty();
        }
        for (double Back : Retreats) {
            Vec3 Spot = holdSpot(Lower, State, Side, Back);
            if (World.noCollision(crouchBox(Spot)) && World.noCollision(shoulderBox(Lower, State, Spot, Side)) && grounded(World, Spot)
                    && !World.containsAnyLiquid(crouchBox(Spot))) {
                return Optional.of(Spot);
            }
        }
        return Optional.empty();
    }

    public static Optional<Vec3> cellIdle(Level World, BlockPos Lower, int Side) {
        BlockState State = World.getBlockState(Lower);
        if (!usable(State)) {
            return Optional.empty();
        }
        double Face = plane(Lower, State) + Side * SlabDepth / 2.0;
        for (double Depth : CellDepths) {
            for (int Lateral : CellLaterals) {
                for (int Dy = 0; Dy >= -1; Dy--) {
                    Vec3 Spot = along(Lower, State, Face + Side * Depth, Lateral, Lower.getY() + Dy);
                    if (World.noCollision(standBox(Spot)) && grounded(World, Spot) && !World.containsAnyLiquid(standBox(Spot))) {
                        return Optional.of(Spot);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
