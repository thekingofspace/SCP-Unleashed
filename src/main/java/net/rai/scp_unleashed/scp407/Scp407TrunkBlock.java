package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class Scp407TrunkBlock extends Scp407GrowthBlock {
    public static final Map<Direction, BooleanProperty> Links = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final double Low = 3.0;
    public static final double High = 13.0;
    private static final VoxelShape Core = box(Low, Low, Low, High, High, High);
    private static final Map<Direction, VoxelShape> Arms = facing(Low, 0.0, Low, High, Low, High);
    private static final VoxelShape[] Outlines = new VoxelShape[64];

    public Scp407TrunkBlock(Properties Settings) {
        super(Settings);
        BlockState Start = stateDefinition.any().setValue(Wither, 0).setValue(Carpet, false);
        for (Direction Side : Direction.values()) {
            Start = Start.setValue(Links.get(Side), Side.getAxis() == Direction.Axis.Y);
        }
        registerDefaultState(Start);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Carpet);
        for (Direction Side : Direction.values()) {
            Builder.add(Links.get(Side));
        }
    }

    public static boolean linked(BlockState State, Direction Side) {
        return State.getBlock() instanceof Scp407TrunkBlock && State.getValue(Links.get(Side));
    }

    public static int linkCount(BlockState State) {
        int Count = 0;
        for (Direction Side : Direction.values()) {
            if (linked(State, Side)) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean joins(LevelReader World, BlockPos Pos, Direction Side, BlockState Other) {
        if (Other.getBlock() instanceof Scp407TrunkBlock) {
            return true;
        }
        if (Other.getBlock() instanceof Scp407GrowthBlock Growth && !Growth.soft(Other) && !(Other.getBlock() instanceof Scp407PodBlock)) {
            Direction Toward = Growth.base(Other);
            if (Toward == Side.getOpposite()) {
                return true;
            }
        }
        if (Other.getBlock() instanceof Scp407PlantBlock Plant && Plant.base(Other) == Side.getOpposite()) {
            return true;
        }
        return Side.getAxis() == Direction.Axis.Y && Scp407Faces.holds(World, Pos, Side);
    }

    public static BlockState connect(LevelReader World, BlockPos Pos, BlockState State) {
        BlockState Out = State;
        for (Direction Side : Direction.values()) {
            Out = Out.setValue(Links.get(Side), joins(World, Pos, Side, World.getBlockState(Pos.relative(Side))));
        }
        return Out;
    }

    @Override
    public BlockState dress(LevelReader World, BlockPos Pos, BlockState State) {
        BlockState Out = connect(World, Pos, State);
        return Out.setValue(Carpet, Out.getValue(Links.get(Direction.DOWN)) && Scp407Faces.holds(World, Pos, Direction.DOWN));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (!alive(State)) {
            return State;
        }
        BlockState Out = State.setValue(Links.get(Side), joins(World, Pos, Side, Neighbour));
        if (Side == Direction.DOWN) {
            Out = Out.setValue(Carpet, Out.getValue(Links.get(Direction.DOWN)) && Scp407Faces.holds(World, Pos, Direction.DOWN));
        }
        return Out;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        int Mask = 0;
        for (Direction Side : Direction.values()) {
            if (State.getValue(Links.get(Side))) {
                Mask |= 1 << Side.ordinal();
            }
        }
        VoxelShape Found = Outlines[Mask];
        if (Found == null) {
            Found = Core;
            for (Direction Side : Direction.values()) {
                if ((Mask & (1 << Side.ordinal())) != 0) {
                    Found = Shapes.or(Found, Arms.get(Side));
                }
            }
            Outlines[Mask] = Found;
        }
        return Found;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        BlockState Out = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Out = Out.setValue(Links.get(Turn.rotate(Side)), State.getValue(Links.get(Side)));
        }
        return Out;
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        BlockState Out = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Out = Out.setValue(Links.get(Flip.mirror(Side)), State.getValue(Links.get(Side)));
        }
        return Out;
    }

    @Override
    protected float moteChance(BlockState State) {
        return 0.05f;
    }
}
