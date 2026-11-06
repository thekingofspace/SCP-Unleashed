package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Scp407Faces {
    public static final Map<Direction, BooleanProperty> Sides = PipeBlock.PROPERTY_BY_DIRECTION;

    private Scp407Faces() {
    }

    public static boolean has(BlockState State, Direction Side) {
        BooleanProperty Face = Sides.get(Side);
        return State.hasProperty(Face) && State.getValue(Face);
    }

    public static boolean allows(BlockState State, Direction Side) {
        return State.hasProperty(Sides.get(Side));
    }

    public static BlockState with(BlockState State, Direction Side, boolean On) {
        BooleanProperty Face = Sides.get(Side);
        return State.hasProperty(Face) ? State.setValue(Face, On) : State;
    }

    public static boolean holds(BlockGetter World, BlockPos Pos, Direction Side) {
        BlockPos Support = Pos.relative(Side);
        return World.getBlockState(Support).isFaceSturdy(World, Support, Side.getOpposite());
    }

    public static int count(BlockState State) {
        int Count = 0;
        for (Direction Side : Direction.values()) {
            if (has(State, Side)) {
                Count++;
            }
        }
        return Count;
    }

    public static int mask(BlockState State) {
        int Mask = 0;
        for (Direction Side : Direction.values()) {
            if (has(State, Side)) {
                Mask |= 1 << Side.ordinal();
            }
        }
        return Mask;
    }

    public static List<Direction> open(BlockGetter World, BlockPos Pos, BlockState State) {
        List<Direction> Found = new ArrayList<>();
        for (Direction Side : Direction.values()) {
            if (allows(State, Side) && !has(State, Side) && holds(World, Pos, Side)) {
                Found.add(Side);
            }
        }
        return Found;
    }

    public static BlockState prune(BlockGetter World, BlockPos Pos, BlockState State) {
        BlockState Out = State;
        for (Direction Side : Direction.values()) {
            if (has(Out, Side) && !holds(World, Pos, Side)) {
                Out = with(Out, Side, false);
            }
        }
        return Out;
    }

    public static VoxelShape union(VoxelShape Base, Map<Direction, VoxelShape> Pads, BlockState State) {
        VoxelShape Out = Base;
        for (Direction Side : Direction.values()) {
            if (has(State, Side)) {
                Out = Shapes.or(Out, Pads.get(Side));
            }
        }
        return Out;
    }

    public static BlockState rotate(BlockState State, Rotation Turn) {
        BlockState Out = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (allows(State, Side)) {
                Out = with(Out, Turn.rotate(Side), has(State, Side));
            }
        }
        return Out;
    }

    public static BlockState mirror(BlockState State, Mirror Flip) {
        BlockState Out = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (allows(State, Side)) {
                Out = with(Out, Flip.mirror(Side), has(State, Side));
            }
        }
        return Out;
    }
}
