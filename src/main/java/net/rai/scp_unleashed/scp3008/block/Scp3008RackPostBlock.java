package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Scp3008RackPostBlock extends Block {
    public static final MapCodec<Scp3008RackPostBlock> PostCodec = simpleCodec(Scp3008RackPostBlock::new);
    public static final EnumProperty<Direction.Axis> Axis = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty Low = BooleanProperty.create("low");
    public static final BooleanProperty High = BooleanProperty.create("high");
    public static final BooleanProperty Foot = BooleanProperty.create("foot");
    public static final int AlongZ = 1;
    public static final int LowJoint = 2;
    public static final int HighJoint = 4;
    public static final int Footed = 8;
    public static final double[][] Frame = {{6, 0, 0, 10, 16, 16}};
    public static final double[][] LowBoxes = {{0, 10.5, 0, 6, 16, 2.5}, {0, 10.5, 13.5, 6, 16, 16}};
    public static final double[][] HighBoxes = {{10, 10.5, 0, 16, 16, 2.5}, {10, 10.5, 13.5, 16, 16, 16}};
    private static final Map<Integer, VoxelShape> Shapes = new ConcurrentHashMap<>();

    public Scp3008RackPostBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Axis, Direction.Axis.X).setValue(Low, false).setValue(High, false).setValue(Foot, true));
    }

    @Override
    protected MapCodec<? extends Scp3008RackPostBlock> codec() {
        return PostCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Axis, Low, High, Foot);
    }

    public static int bits(BlockState State) {
        return (State.getValue(Axis) == Direction.Axis.Z ? AlongZ : 0) | (State.getValue(Low) ? LowJoint : 0) | (State.getValue(High) ? HighJoint : 0)
                | (State.getValue(Foot) ? Footed : 0);
    }

    public BlockState fromBits(int Bits) {
        return defaultBlockState().setValue(Axis, (Bits & AlongZ) != 0 ? Direction.Axis.Z : Direction.Axis.X).setValue(Low, (Bits & LowJoint) != 0)
                .setValue(High, (Bits & HighJoint) != 0).setValue(Foot, (Bits & Footed) != 0);
    }

    public static Direction lowSide(Direction.Axis Along) {
        return Along == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
    }

    public static Direction highSide(Direction.Axis Along) {
        return Along == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    public static boolean beam(BlockState State, Direction.Axis Along) {
        return State.getBlock() instanceof Scp3008BeamBlock && State.getValue(Scp3008BeamBlock.Axis) == Along;
    }

    private static boolean beamAt(BlockGetter World, BlockPos Pos, Direction.Axis Along) {
        return beam(World.getBlockState(Pos), Along);
    }

    private boolean post(BlockState State) {
        return State.is(this);
    }

    public BlockState joined(BlockGetter World, BlockPos Pos, BlockState State) {
        boolean OnX = beamAt(World, Pos.west(), Direction.Axis.X) || beamAt(World, Pos.east(), Direction.Axis.X);
        boolean OnZ = beamAt(World, Pos.north(), Direction.Axis.Z) || beamAt(World, Pos.south(), Direction.Axis.Z);
        Direction.Axis Along = State.getValue(Axis);
        if (OnX != OnZ) {
            Along = OnX ? Direction.Axis.X : Direction.Axis.Z;
        }
        return State.setValue(Axis, Along).setValue(Low, beamAt(World, Pos.relative(lowSide(Along)), Along))
                .setValue(High, beamAt(World, Pos.relative(highSide(Along)), Along)).setValue(Foot, !post(World.getBlockState(Pos.below())));
    }

    private boolean braced(BlockGetter World, BlockPos Pos) {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (beamAt(World, Pos.relative(Side), Side.getAxis())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockGetter World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        Direction.Axis Along = Context.getHorizontalDirection().getAxis();
        BlockState Below = World.getBlockState(Pos.below());
        BlockState Above = World.getBlockState(Pos.above());
        if (post(Below)) {
            Along = Below.getValue(Axis);
        } else if (post(Above)) {
            Along = Above.getValue(Axis);
        }
        return joined(World, Pos, defaultBlockState().setValue(Axis, Along));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        BlockState Made = State;
        if (Side.getAxis().isVertical() && post(Neighbour) && !braced(World, Pos)) {
            Made = Made.setValue(Axis, Neighbour.getValue(Axis));
        }
        return joined(World, Pos, Made);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        int Bits = bits(State) & ~Footed;
        return Shapes.computeIfAbsent(Bits, Key -> {
            List<double[]> Boxes = new ArrayList<>(List.of(Frame));
            if ((Key & LowJoint) != 0) {
                Boxes.addAll(List.of(LowBoxes));
            }
            if ((Key & HighJoint) != 0) {
                Boxes.addAll(List.of(HighBoxes));
            }
            double[][] Turned = new double[Boxes.size()][];
            for (int Index = 0; Index < Boxes.size(); Index++) {
                Turned[Index] = (Key & AlongZ) != 0 ? Scp3008Shapes.turn(Boxes.get(Index), Direction.EAST) : Boxes.get(Index);
            }
            return Scp3008Shapes.boxes(Turned);
        });
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        if (Turn == Rotation.NONE) {
            return State;
        }
        Direction.Axis Along = State.getValue(Axis);
        Direction NewLow = Turn.rotate(lowSide(Along));
        Direction.Axis Turned = NewLow.getAxis();
        boolean Kept = NewLow == lowSide(Turned);
        return State.setValue(Axis, Turned).setValue(Low, Kept ? State.getValue(Low) : State.getValue(High)).setValue(High, Kept ? State.getValue(High) : State.getValue(Low));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        Direction.Axis Along = State.getValue(Axis);
        if (Flip.mirror(lowSide(Along)) == lowSide(Along)) {
            return State;
        }
        return State.setValue(Low, State.getValue(High)).setValue(High, State.getValue(Low));
    }
}
