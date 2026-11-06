package net.rai.scp_unleashed.scp261;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Scp261VendingBlock extends BaseEntityBlock implements LargeBlock {
    public static final MapCodec<Scp261VendingBlock> BlockCodec = simpleCodec(Scp261VendingBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final int UpperGlow = 7;
    public static final int LowerGlow = 5;
    public static final String Prefix = "block.scp_unleashed.scp261_vending.";
    private static final Map<Direction, VoxelShape> LowerShapes = turned(box(0, 0, 0.5, 16, 16, 16));
    private static final Map<Direction, VoxelShape> UpperShapes = turned(Shapes.or(box(0, 0, 0.5, 16, 16, 16), box(0.5, 9, 0, 14.5, 15, 0.5)));

    public Scp261VendingBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER));
    }

    private static Map<Direction, VoxelShape> turned(VoxelShape North) {
        Map<Direction, VoxelShape> Result = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            int Turns = switch (Side) {
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0;
            };
            VoxelShape Shape = Shapes.empty();
            for (AABB Box : North.toAabbs()) {
                double MinX = Box.minX;
                double MaxX = Box.maxX;
                double MinZ = Box.minZ;
                double MaxZ = Box.maxZ;
                for (int Step = 0; Step < Turns; Step++) {
                    double TurnedMinX = 1.0 - MaxZ;
                    double TurnedMaxX = 1.0 - MinZ;
                    MinZ = MinX;
                    MaxZ = MaxX;
                    MinX = TurnedMinX;
                    MaxX = TurnedMaxX;
                }
                Shape = Shapes.or(Shape, Shapes.box(MinX, Box.minY, MinZ, MaxX, Box.maxY, MaxZ));
            }
            Result.put(Side, Shape.optimize());
        }
        return Result;
    }

    public static int lightOf(BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? UpperGlow : LowerGlow;
    }

    public static BlockPos lowerPos(BlockState State, BlockPos Pos) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    @Nullable
    public static Scp261VendingBlockEntity machineAt(BlockGetter World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof Scp261VendingBlock)) {
            return null;
        }
        return World.getBlockEntity(lowerPos(State, Pos)) instanceof Scp261VendingBlockEntity Machine ? Machine : null;
    }

    public static boolean isGoldCoin(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.is(Tags.Items.NUGGETS_GOLD);
    }

    public static boolean isCoin(ItemStack Stack) {
        return !Stack.isEmpty() && (Stack.is(Tags.Items.NUGGETS_IRON) || Stack.is(Tags.Items.NUGGETS_GOLD));
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
        return (State.getValue(Half) == DoubleBlockHalf.LOWER ? LowerShapes : UpperShapes).get(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
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
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!isCoin(Stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            Scp261VendingBlockEntity Machine = machineAt(World, Pos, State);
            if (Machine != null) {
                Machine.pay(User, Stack);
            }
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            Scp261VendingBlockEntity Machine = machineAt(World, Pos, State);
            if (Machine != null) {
                User.displayClientMessage(Component.translatable(Machine.isIdle() ? "message.scp_unleashed.scp261.hint" : "message.scp_unleashed.scp261.busy")
                        .withStyle(ChatFormatting.GRAY), true);
                World.playSound(null, Pos, Scp261Registry.Hum.get(), SoundSource.BLOCKS, 0.4f, 0.9f + World.getRandom().nextFloat() * 0.2f);
            }
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
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "pay").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "wait").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "size").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? new Scp261VendingBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide || State.getValue(Half) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return createTickerHelper(Type, Scp261Registry.VendingEntity.get(), Scp261VendingBlockEntity::serverTick);
    }
}
