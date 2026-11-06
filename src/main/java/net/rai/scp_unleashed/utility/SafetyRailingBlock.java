package net.rai.scp_unleashed.utility;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SafetyRailingBlock extends UtilityWaterloggedBlock {
    public static final MapCodec<SafetyRailingBlock> BlockCodec = simpleCodec(SafetyRailingBlock::new);
    public static final BooleanProperty North = PipeBlock.NORTH;
    public static final BooleanProperty East = PipeBlock.EAST;
    public static final BooleanProperty South = PipeBlock.SOUTH;
    public static final BooleanProperty West = PipeBlock.WEST;
    public static final BooleanProperty PostNorthEast = BooleanProperty.create("post_ne");
    public static final BooleanProperty PostSouthEast = BooleanProperty.create("post_se");
    public static final BooleanProperty PostSouthWest = BooleanProperty.create("post_sw");
    public static final BooleanProperty PostNorthWest = BooleanProperty.create("post_nw");
    public static final BooleanProperty Catwalk = BooleanProperty.create("catwalk");
    public static final double Depth = 2.0;
    public static final double OutlineHeight = 16.0;
    public static final double CollisionHeight = 24.0;
    private static final Map<Direction, BooleanProperty> Edges = new EnumMap<>(Direction.class);
    private static final Map<Direction, BooleanProperty> Posts = new EnumMap<>(Direction.class);
    private static final VoxelShape[] Outlines = new VoxelShape[512];
    private static final VoxelShape[] Collisions = new VoxelShape[512];

    static {
        Edges.put(Direction.NORTH, North);
        Edges.put(Direction.EAST, East);
        Edges.put(Direction.SOUTH, South);
        Edges.put(Direction.WEST, West);
        Posts.put(Direction.NORTH, PostNorthEast);
        Posts.put(Direction.EAST, PostSouthEast);
        Posts.put(Direction.SOUTH, PostSouthWest);
        Posts.put(Direction.WEST, PostNorthWest);
        for (int Index = 0; Index < Outlines.length; Index++) {
            Outlines[Index] = buildShape(Index, OutlineHeight);
            Collisions[Index] = buildShape(Index, CollisionHeight);
        }
    }

    public SafetyRailingBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(North, true).setValue(East, false).setValue(South, false).setValue(West, false)
                .setValue(PostNorthEast, true).setValue(PostSouthEast, false).setValue(PostSouthWest, false).setValue(PostNorthWest, true)
                .setValue(Catwalk, false));
    }

    public static BooleanProperty edge(Direction Side) {
        return Edges.get(Side);
    }

    public static BooleanProperty post(Direction Side) {
        return Posts.get(Side);
    }

    @Override
    protected MapCodec<? extends SafetyRailingBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(North, East, South, West, PostNorthEast, PostSouthEast, PostSouthWest, PostNorthWest, Catwalk);
    }

    private static int shapeIndex(BlockState State) {
        int Index = 0;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (State.getValue(edge(Side))) {
                Index |= 1 << Side.get2DDataValue();
            }
            if (State.getValue(post(Side))) {
                Index |= 16 << Side.get2DDataValue();
            }
        }
        return State.getValue(Catwalk) ? Index | 256 : Index;
    }

    private static VoxelShape buildShape(int Index, double Height) {
        VoxelShape Shape = (Index & 256) != 0 ? CatwalkBlock.Plate : Shapes.empty();
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if ((Index & (1 << Side.get2DDataValue())) != 0) {
                Shape = Shapes.or(Shape, edgeBox(Side, Height));
            }
            if ((Index & (16 << Side.get2DDataValue())) != 0) {
                Shape = Shapes.or(Shape, cornerBox(Side, Height));
            }
        }
        return Shape.optimize();
    }

    private static VoxelShape edgeBox(Direction Side, double Height) {
        return switch (Side) {
            case NORTH -> Block.box(0, 0, 0, 16, Height, Depth);
            case SOUTH -> Block.box(0, 0, 16 - Depth, 16, Height, 16);
            case WEST -> Block.box(0, 0, 0, Depth, Height, 16);
            default -> Block.box(16 - Depth, 0, 0, 16, Height, 16);
        };
    }

    private static VoxelShape cornerBox(Direction Side, double Height) {
        Direction Next = Side.getClockWise();
        double X0 = Side == Direction.EAST || Next == Direction.EAST ? 16 - Depth : 0;
        double Z0 = Side == Direction.SOUTH || Next == Direction.SOUTH ? 16 - Depth : 0;
        return Block.box(X0, 0, Z0, X0 + Depth, Height, Z0 + Depth);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines[shapeIndex(State)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Collisions[shapeIndex(State)];
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    private static boolean hasEdge(BlockState State, Direction Side) {
        return State.getBlock() instanceof SafetyRailingBlock && State.getValue(edge(Side));
    }

    private static boolean hasPost(BlockState State, Direction Side) {
        return State.getBlock() instanceof SafetyRailingBlock && State.getValue(post(Side));
    }

    public static BlockState withPosts(BlockState State, BlockGetter World, BlockPos Pos) {
        boolean Bare = edgeCount(State) == 0;
        boolean Any = false;
        BlockState Result = State;
        for (Direction First : Direction.Plane.HORIZONTAL) {
            Direction Second = First.getClockWise();
            boolean Needed = Bare ? cornerPost(World, Pos, First, Second) : needsPost(State, World, Pos, First, Second);
            Result = Result.setValue(post(First), Needed);
            Any |= Needed;
        }
        return Bare && !Any && !State.getValue(Catwalk) ? State : Result;
    }

    private static boolean cornerPost(BlockGetter World, BlockPos Pos, Direction First, Direction Second) {
        return hasEdge(World.getBlockState(Pos.relative(First)), Second) || hasEdge(World.getBlockState(Pos.relative(Second)), First);
    }

    private static boolean hasAnyPost(BlockState State) {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (State.getValue(post(Side))) {
                return true;
            }
        }
        return false;
    }

    private static boolean needsPost(BlockState State, BlockGetter World, BlockPos Pos, Direction First, Direction Second) {
        boolean HasFirst = State.getValue(edge(First));
        boolean HasSecond = State.getValue(edge(Second));
        if (HasFirst && HasSecond) {
            return true;
        }
        if (HasFirst) {
            BlockState Along = World.getBlockState(Pos.relative(Second));
            return !hasEdge(Along, First) && !hasPost(Along, First.getCounterClockWise());
        }
        if (HasSecond) {
            BlockState Along = World.getBlockState(Pos.relative(First));
            return !hasPost(Along, Second);
        }
        return hasEdge(World.getBlockState(Pos.relative(First)), Second) && hasEdge(World.getBlockState(Pos.relative(Second)), First);
    }

    public static int edgeCount(BlockState State) {
        int Count = 0;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (State.getValue(edge(Side))) {
                Count++;
            }
        }
        return Count;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        BlockState Existing = Context.getLevel().getBlockState(Pos);
        BlockState State;
        if (Existing.is(this)) {
            State = Existing;
        } else {
            State = watered(defaultBlockState(), Context).setValue(North, false).setValue(Catwalk, Existing.getBlock() instanceof CatwalkBlock);
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                State = State.setValue(post(Side), false);
            }
            if (Context.isSecondaryUseActive()) {
                BlockState Posted = withPosts(State, Context.getLevel(), Pos);
                return hasAnyPost(Posted) ? Posted : null;
            }
        }
        State = State.setValue(edge(Context.getHorizontalDirection()), true);
        return withPosts(State, Context.getLevel(), Pos);
    }

    @Override
    protected boolean canBeReplaced(BlockState State, BlockPlaceContext Context) {
        if (!Context.isSecondaryUseActive() && Context.getItemInHand().is(asItem())) {
            return !State.getValue(edge(Context.getHorizontalDirection()));
        }
        if (!State.getValue(Catwalk) && Context.getItemInHand().getItem() instanceof BlockItem Held && Held.getBlock() instanceof CatwalkBlock) {
            return true;
        }
        return super.canBeReplaced(State, Context);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        BlockState Updated = super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
        return Side.getAxis().isHorizontal() ? withPosts(Updated, World, Pos) : Updated;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        BlockState Result = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Result = Result.setValue(edge(Turn.rotate(Side)), State.getValue(edge(Side)));
            Result = Result.setValue(post(Turn.rotate(Side)), State.getValue(post(Side)));
        }
        return Result;
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        if (Flip == Mirror.NONE) {
            return State;
        }
        BlockState Result = State;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Direction Mirrored = Flip.mirror(Side);
            Result = Result.setValue(edge(Mirrored), State.getValue(edge(Side)));
            Result = Result.setValue(post(Mirrored.getCounterClockWise()), State.getValue(post(Side)));
        }
        return Result;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "block.scp_unleashed.safety_railing.edges", "block.scp_unleashed.safety_railing.corners", "block.scp_unleashed.safety_railing.post",
                "block.scp_unleashed.safety_railing.catwalk");
    }
}
