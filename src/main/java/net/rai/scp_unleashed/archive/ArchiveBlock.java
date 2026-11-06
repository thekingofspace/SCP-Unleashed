package net.rai.scp_unleashed.archive;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ArchiveBlock extends BaseEntityBlock implements LargeBlock {
    public static final MapCodec<ArchiveBlock> BlockCodec = simpleCodec(ArchiveBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final double[][] LowerBoxes = {
            {0, 0, 0, 16, 1, 16},
            {1, 1, 1, 15, 16, 16}
    };
    private static final double[][] UpperBoxes = {
            {2, 0, 1, 14, 1.5, 4},
            {2, 0, 4, 14, 13, 13},
            {4, 2, 13, 12, 11, 16}
    };
    private static final Map<Direction, VoxelShape> LowerShapes = shapesFor(LowerBoxes);
    private static final Map<Direction, VoxelShape> UpperShapes = shapesFor(UpperBoxes);

    public ArchiveBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER));
    }

    private static Map<Direction, VoxelShape> shapesFor(double[][] Boxes) {
        Map<Direction, VoxelShape> Turned = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            VoxelShape Shape = Shapes.empty();
            for (double[] Box : Boxes) {
                Shape = Shapes.or(Shape, turned(Side, Box));
            }
            Turned.put(Side, Shape.optimize());
        }
        return Turned;
    }

    private static VoxelShape turned(Direction Side, double[] Box) {
        double[] First = turnPoint(Side, Box[0], Box[2]);
        double[] Second = turnPoint(Side, Box[3], Box[5]);
        return Block.box(Math.min(First[0], Second[0]), Box[1], Math.min(First[1], Second[1]), Math.max(First[0], Second[0]), Box[4], Math.max(First[1], Second[1]));
    }

    private static double[] turnPoint(Direction Side, double X, double Z) {
        return switch (Side) {
            case EAST -> new double[]{16 - Z, X};
            case SOUTH -> new double[]{16 - X, 16 - Z};
            case WEST -> new double[]{Z, 16 - X};
            default -> new double[]{X, Z};
        };
    }

    public static BlockPos lowerPos(BlockState State, BlockPos Pos) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Half);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
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
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && State.getValue(Half) == DoubleBlockHalf.LOWER && World.getBlockEntity(Pos) instanceof ArchiveBlockEntity Terminal) {
            Terminal.dropContents(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && User instanceof ServerPlayer Reader && World.getBlockEntity(lowerPos(State, Pos)) instanceof ArchiveBlockEntity Terminal) {
            Terminal.openFor(Reader);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
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
        Lines.add(Component.translatable("block.scp_unleashed.archive.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.archive.cost", ArchiveBlockEntity.PaperCost).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.archive.cards", ArchiveBlockEntity.CardCount).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.archive.rarity").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? new ArchiveBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide || State.getValue(Half) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return createTickerHelper(Type, ArchiveRegistry.ArchiveEntity.get(), ArchiveBlockEntity::serverTick);
    }
}
