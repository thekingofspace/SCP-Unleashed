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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Scp3008RafterBlock extends Block {
    public static final MapCodec<Scp3008RafterBlock> RafterCodec = simpleCodec(Scp3008RafterBlock::new);
    public static final BooleanProperty North = BlockStateProperties.NORTH;
    public static final BooleanProperty East = BlockStateProperties.EAST;
    public static final BooleanProperty South = BlockStateProperties.SOUTH;
    public static final BooleanProperty West = BlockStateProperties.WEST;
    public static final int NorthArm = 1;
    public static final int EastArm = 2;
    public static final int SouthArm = 4;
    public static final int WestArm = 8;
    public static final double Thickness = 5.0;
    private static final VoxelShape[] Outlines = outlines();

    public Scp3008RafterBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(North, false).setValue(East, false).setValue(South, false).setValue(West, false));
    }

    @Override
    protected MapCodec<? extends Scp3008RafterBlock> codec() {
        return RafterCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(North, East, South, West);
    }

    public static BooleanProperty arm(Direction Side) {
        return switch (Side) {
            case NORTH -> North;
            case EAST -> East;
            case SOUTH -> South;
            default -> West;
        };
    }

    public static int bit(Direction Side) {
        return switch (Side) {
            case NORTH -> NorthArm;
            case EAST -> EastArm;
            case SOUTH -> SouthArm;
            default -> WestArm;
        };
    }

    public static int arms(BlockState State) {
        int Mask = 0;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (State.getValue(arm(Side))) {
                Mask |= bit(Side);
            }
        }
        return Mask;
    }

    public static boolean reaches(BlockState State, Direction Side) {
        return State.getBlock() instanceof Scp3008RafterBlock && State.getValue(arm(Side));
    }

    public BlockState withArms(int Mask) {
        BlockState Made = defaultBlockState();
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Made = Made.setValue(arm(Side), (Mask & bit(Side)) != 0);
        }
        return Made;
    }

    private static VoxelShape[] outlines() {
        VoxelShape[] Made = new VoxelShape[16];
        for (int Mask = 0; Mask < 16; Mask++) {
            VoxelShape Shape = Block.box(5, 0, 5, 11, Thickness, 11);
            if ((Mask & NorthArm) != 0) {
                Shape = Shapes.or(Shape, Block.box(5, 0, 0, 11, Thickness, 5));
            }
            if ((Mask & EastArm) != 0) {
                Shape = Shapes.or(Shape, Block.box(11, 0, 5, 16, Thickness, 11));
            }
            if ((Mask & SouthArm) != 0) {
                Shape = Shapes.or(Shape, Block.box(5, 0, 11, 11, Thickness, 16));
            }
            if ((Mask & WestArm) != 0) {
                Shape = Shapes.or(Shape, Block.box(0, 0, 5, 5, Thickness, 11));
            }
            Made[Mask] = Shape.optimize();
        }
        return Made;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines[arms(State)];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Made = defaultBlockState();
        boolean Any = false;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            BlockPos Next = Context.getClickedPos().relative(Side);
            BlockState There = Context.getLevel().getBlockState(Next);
            boolean Joined = There.getBlock() instanceof Scp3008RafterBlock || There.isFaceSturdy(Context.getLevel(), Next, Side.getOpposite());
            Made = Made.setValue(arm(Side), Joined);
            Any |= Joined;
        }
        if (!Any) {
            Direction Facing = Context.getHorizontalDirection();
            Made = Made.setValue(arm(Facing), true).setValue(arm(Facing.getOpposite()), true);
        }
        return Made;
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (!Side.getAxis().isHorizontal()) {
            return State;
        }
        if (Neighbour.getBlock() instanceof Scp3008RafterBlock) {
            return State.setValue(arm(Side), Neighbour.getValue(arm(Side.getOpposite())));
        }
        if (Neighbour.isFaceSturdy(World, NeighbourPos, Side.getOpposite())) {
            return State;
        }
        return State.setValue(arm(Side), false);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        BlockState Made = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Made = Made.setValue(arm(Turn.rotate(Side)), State.getValue(arm(Side)));
        }
        return Made;
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        BlockState Made = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Made = Made.setValue(arm(Flip.mirror(Side)), State.getValue(arm(Side)));
        }
        return Made;
    }
}
