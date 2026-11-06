package net.rai.scp_unleashed.scp343;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.furnishing.SeatHost;
import net.rai.scp_unleashed.furnishing.Seating;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Scp343ThroneBlock extends Block implements LargeBlock, SeatHost {
    public static final MapCodec<Scp343ThroneBlock> BlockCodec = simpleCodec(Scp343ThroneBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final double SeatHeight = 9.5 / 16.0;
    private static final double[][] LowerBoxes = {
            {0, 0, 0, 16, 2, 16},
            {2, 2, 2, 14, 9.5, 14},
            {0.5, 2, 1.5, 3.5, 12, 15.5},
            {12.5, 2, 1.5, 15.5, 12, 15.5},
            {1.5, 2, 12.5, 14.5, 16, 16}
    };
    private static final double[][] UpperBoxes = {
            {0.25, 0, 12.5, 15.75, 14.5, 16},
            {6.5, 14.5, 13.5, 9.5, 15.5, 15}
    };
    private static final Map<Direction, VoxelShape> LowerShapes = turned(LowerBoxes);
    private static final Map<Direction, VoxelShape> UpperShapes = turned(UpperBoxes);

    public Scp343ThroneBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER));
    }

    private static Map<Direction, VoxelShape> turned(double[][] Boxes) {
        Map<Direction, VoxelShape> Found = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            VoxelShape Shape = Shapes.empty();
            for (double[] Box : Boxes) {
                Shape = Shapes.or(Shape, turn(Box, Side));
            }
            Found.put(Side, Shape.optimize());
        }
        return Found;
    }

    private static VoxelShape turn(double[] Box, Direction Side) {
        double X0 = Box[0];
        double Z0 = Box[2];
        double X1 = Box[3];
        double Z1 = Box[5];
        double[] Turned = switch (Side) {
            case SOUTH -> new double[]{16 - X1, 16 - Z1, 16 - X0, 16 - Z0};
            case EAST -> new double[]{16 - Z1, X0, 16 - Z0, X1};
            case WEST -> new double[]{Z0, 16 - X1, Z1, 16 - X0};
            default -> new double[]{X0, Z0, X1, Z1};
        };
        return Block.box(Turned[0], Box[1], Turned[1], Turned[2], Box[4], Turned[3]);
    }

    public static BlockPos lowerPos(BlockState State, BlockPos Pos) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Half);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Half) == DoubleBlockHalf.UPPER ? UpperShapes : LowerShapes).get(State.getValue(Facing));
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.above());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        World.setBlock(Pos.above(), State.setValue(Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        DoubleBlockHalf ThisHalf = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (ThisHalf == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            boolean Partner = NeighborState.is(this) && NeighborState.getValue(Half) != ThisHalf;
            return Partner ? State : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Half) == DoubleBlockHalf.UPPER) {
            BlockPos LowerPos = Pos.below();
            BlockState LowerState = World.getBlockState(LowerPos);
            if (LowerState.is(this) && LowerState.getValue(Half) == DoubleBlockHalf.LOWER) {
                World.setBlock(LowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, LowerPos, Block.getId(LowerState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean Moved) {
        super.onPlace(State, World, Pos, OldState, Moved);
        if (World instanceof ServerLevel Server && State.getValue(Half) == DoubleBlockHalf.LOWER) {
            Scp343Data.get(Server.getServer()).addThrone(Server.dimension(), Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean Moved) {
        if (World instanceof ServerLevel Server && State.getValue(Half) == DoubleBlockHalf.LOWER && !(NewState.is(this) && NewState.getValue(Half) == DoubleBlockHalf.LOWER)) {
            Scp343Data.get(Server.getServer()).removeThrone(Server.dimension(), Pos);
        }
        super.onRemove(State, World, Pos, NewState, Moved);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        BlockPos Seat = lowerPos(State, Pos);
        BlockState SeatState = World.getBlockState(Seat);
        if (!SeatState.is(this)) {
            return InteractionResult.PASS;
        }
        return Seating.sit(World, Seat, SeatState, User);
    }

    @Override
    public double seatHeight(BlockState State) {
        return SeatHeight;
    }

    @Override
    public float seatYaw(BlockState State, BlockPos Pos, Player User) {
        return State.getValue(Facing).toYRot();
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp343_throne.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp343_throne.sit").withStyle(ChatFormatting.GOLD));
        }
    }
}
